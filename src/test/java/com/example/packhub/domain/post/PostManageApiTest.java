package com.example.packhub.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.post.dto.RequestSaveRequest;
import com.example.packhub.domain.post.entity.AttachType;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostAttachment;
import com.example.packhub.domain.post.entity.PostRequest;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.repository.PostAttachmentRepository;
import com.example.packhub.domain.post.repository.PostRepository;
import com.example.packhub.domain.post.repository.PostRequestRepository;
import com.example.packhub.domain.post.service.RequestService;
import com.example.packhub.global.security.jwt.JwtProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 의뢰 수정(5-5), 게시글 마감·재게시(9-2)·삭제, 내 게시글 목록(10-1).
 * 의뢰는 등록 서비스로 만들고, API로 만들 수 없는 상태(숨김 등)는 DB를 직접 고친다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PostManageApiTest {

	private static final long SUB_CATEGORY = 105L;
	private static final long OTHER_SUB_CATEGORY = 101L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RequestService requestService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostRequestRepository postRequestRepository;

	@Autowired
	private PostAttachmentRepository postAttachmentRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private FileRepository fileRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long owner;
	private Long other;

	@BeforeEach
	void setUp() {
		owner = saveMember("pm-owner@test.com", "01094100001");
		other = saveMember("pm-other@test.com", "01094100002");
		saveCompany(owner, "팩허브 푸드");
		saveCompany(other, "오렌지 패키징");
	}

	@AfterEach
	void cleanUp() {
		postAttachmentRepository.deleteAllInBatch();
		postRequestRepository.deleteAllInBatch();
		postRepository.deleteAllInBatch();
		companyRepository.deleteAllInBatch();
		fileRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	// ===== 의뢰 수정 =====

	@Test
	void 수정하면_공통정보와_의뢰정보가_바뀌고_첨부는_보낸_목록으로_바뀐다() throws Exception {
		Long oldFile = saveFile(owner, FilePurpose.POST_IMAGE);
		Long keptFile = saveFile(owner, FilePurpose.POST_DOCUMENT);
		Long newFile = saveFile(owner, FilePurpose.POST_IMAGE);
		Long postId = create(owner, "수정 전 제목", List.of(oldFile, keptFile));

		update(owner, postId, body("수정한 제목", OTHER_SUB_CATEGORY, false, 70000, List.of(newFile, keptFile)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.postId").value(postId))
			.andExpect(jsonPath("$.data.status").value("OPEN"))
			.andExpect(jsonPath("$.data.updatedAt").exists());

		Post post = postRepository.findById(postId).orElseThrow();
		assertThat(post.getTitle()).isEqualTo("수정한 제목");
		assertThat(post.getCategoryId()).isEqualTo(OTHER_SUB_CATEGORY);
		assertThat(post.isContactPublic()).isFalse();
		assertThat(post.getMemberId()).isEqualTo(owner);
		PostRequest detail = postRequestRepository.findById(postId).orElseThrow();
		assertThat(detail.getQuantity()).isEqualTo(70000);
		assertThat(postAttachmentRepository.findByPostIdOrderBySortOrderAsc(postId))
			.extracting(PostAttachment::getFileId, PostAttachment::getAttachType, PostAttachment::getSortOrder)
			.containsExactly(tuple(newFile, AttachType.IMAGE, 1), tuple(keptFile, AttachType.DOCUMENT, 2));
	}

	@Test
	void 남의_의뢰를_수정하면_403이고_바뀌지_않는다() throws Exception {
		Long postId = create(owner, "원래 제목", List.of());

		update(other, postId, body("바꾼 제목", SUB_CATEGORY, true, 50000, List.of()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		assertThat(postRepository.findById(postId).orElseThrow().getTitle()).isEqualTo("원래 제목");
	}

	@Test
	void 마감된_의뢰는_수정할_수_없다() throws Exception {
		Long postId = create(owner, "마감 의뢰", List.of());
		changeStatus(owner, postId, "CLOSED").andExpect(status().isOk());

		update(owner, postId, body("바꾼 제목", SUB_CATEGORY, true, 50000, List.of()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("POST_ALREADY_CLOSED"));
	}

	@Test
	void 없거나_삭제된_의뢰를_수정하면_404다() throws Exception {
		Long postId = create(owner, "삭제할 의뢰", List.of());
		deletePost(owner, postId).andExpect(status().isOk());

		for (Long id : List.of(postId, 999999L)) {
			update(owner, id, body("바꾼 제목", SUB_CATEGORY, true, 50000, List.of()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
		}
	}

	@Test
	void 수정도_빈_칸이_있으면_400이다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		update(owner, postId, body("", SUB_CATEGORY, true, 50000, List.of()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));
	}

	@Test
	void 로그인하지_않으면_수정_상태변경_삭제_내게시글_모두_401이다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		mockMvc.perform(put("/api/v1/requests/{postId}", postId).contentType(MediaType.APPLICATION_JSON)
			.content(body("제목", SUB_CATEGORY, true, 50000, List.of()))).andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/v1/posts/{postId}/status", postId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"CLOSED\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/posts/{postId}", postId)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/members/me/posts")).andExpect(status().isUnauthorized());
	}

	// ===== 마감·재게시 =====

	@Test
	void 마감하면_CLOSED와_마감시각이_남고_재게시하면_OPEN으로_돌아온다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		changeStatus(owner, postId, "CLOSED")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("CLOSED"))
			.andExpect(jsonPath("$.data.closedAt").exists());
		assertThat(postRepository.findById(postId).orElseThrow().getClosedAt()).isNotNull();

		changeStatus(owner, postId, "OPEN")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("OPEN"))
			.andExpect(jsonPath("$.data.closedAt").value(nullValue()));
		Post post = postRepository.findById(postId).orElseThrow();
		assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
		assertThat(post.getClosedAt()).isNull();
	}

	@Test
	void 지금과_같은_상태로_바꾸면_409다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		changeStatus(owner, postId, "OPEN")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("INVALID_POST_STATUS_TRANSITION"));
	}

	@Test
	void 남의_글_상태를_바꾸면_403이고_없는_상태값은_400이다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		changeStatus(other, postId, "CLOSED")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		changeStatus(owner, postId, "DRAFT")
			.andExpect(status().isBadRequest());
		assertThat(postRepository.findById(postId).orElseThrow().getStatus()).isEqualTo(PostStatus.OPEN);
	}

	// ===== 삭제 =====

	@Test
	void 삭제하면_상세와_내_게시글에서_빠지고_다시_삭제하면_404다() throws Exception {
		Long postId = create(owner, "삭제할 의뢰", List.of());

		deletePost(owner, postId).andExpect(status().isOk());

		assertThat(postRepository.findById(postId).orElseThrow().getDeletedAt()).isNotNull();
		mockMvc.perform(get("/api/v1/requests/{postId}", postId)).andExpect(status().isNotFound());
		myPosts(owner, "").andExpect(jsonPath("$.data.totalElements").value(0));
		deletePost(owner, postId)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
	}

	@Test
	void 남의_글을_삭제하면_403이고_지워지지_않는다() throws Exception {
		Long postId = create(owner, "의뢰", List.of());

		deletePost(other, postId)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		assertThat(postRepository.findById(postId).orElseThrow().getDeletedAt()).isNull();
	}

	// ===== 내 게시글 =====

	@Test
	void 내_게시글은_내가_쓴_글만_최근_순으로_나오고_숨긴_글은_visibility로_알려_준다() throws Exception {
		Long first = create(owner, "첫째", List.of());
		Long hidden = create(owner, "숨김", List.of());
		Long third = create(owner, "셋째", List.of());
		create(other, "남의 글", List.of());
		jdbcTemplate.update("UPDATE Post SET visibility = 'HIDDEN' WHERE postId = ?", hidden);

		myPosts(owner, "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].postId")
				.value(contains(third.intValue(), hidden.intValue(), first.intValue())))
			.andExpect(jsonPath("$.data.content[0].postType").value("REQUEST"))
			.andExpect(jsonPath("$.data.content[0].title").value("셋째"))
			.andExpect(jsonPath("$.data.content[0].status").value("OPEN"))
			.andExpect(jsonPath("$.data.content[0].createdAt").exists())
			.andExpect(jsonPath("$.data.content[1].visibility").value("HIDDEN"))
			.andExpect(jsonPath("$.data.size").value(20));
	}

	@Test
	void 내_게시글은_상태와_종류로_거르고_페이지를_나눈다() throws Exception {
		Long open = create(owner, "모집중", List.of());
		Long closed = create(owner, "마감", List.of());
		changeStatus(owner, closed, "CLOSED").andExpect(status().isOk());

		myPosts(owner, "?status=CLOSED")
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(closed.intValue())));
		myPosts(owner, "?status=OPEN&postType=REQUEST")
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(open.intValue())));
		myPosts(owner, "?postType=JOB_OFFER")
			.andExpect(jsonPath("$.data.totalElements").value(0));
		myPosts(owner, "?page=2&size=1")
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(open.intValue())))
			.andExpect(jsonPath("$.data.totalPages").value(2));
		myPosts(owner, "?page=0")
			.andExpect(status().isBadRequest());
	}

	private ResultActions update(long memberId, long postId, String body) throws Exception {
		return mockMvc.perform(put("/api/v1/requests/{postId}", postId)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions changeStatus(long memberId, long postId, String status) throws Exception {
		return mockMvc.perform(patch("/api/v1/posts/{postId}/status", postId)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"" + status + "\"}"));
	}

	private ResultActions deletePost(long memberId, long postId) throws Exception {
		return mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId)));
	}

	private ResultActions myPosts(long memberId, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/members/me/posts" + query)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId)));
	}

	private Long create(Long memberId, String title, List<Long> fileIds) {
		return requestService.create(memberId, request(title, SUB_CATEGORY, true, 50000, fileIds)).postId();
	}

	private RequestSaveRequest request(String title, long categoryId, boolean isContactPublic, int quantity,
		List<Long> fileIds) {
		LocalDate date = LocalDate.now().plusDays(30);
		return new RequestSaveRequest(categoryId, title, "상세정보입니다.", isContactPublic, "지퍼 파우치", quantity, "매",
			6500000L, "지퍼스탠딩 파우치", "견과류 100g", "160mm x 230mm", "총 104u", "앞면 6도", "PET 12u", "제공 가능",
			"제공 가능", date, date, "경기 파주", "1회 일괄 납품", "부가세 별도", "협의 가능", fileIds);
	}

	/** 수정 요청 본문. 등록과 같은 모양이다 */
	private String body(String title, long categoryId, boolean isContactPublic, int quantity, List<Long> fileIds) {
		String date = LocalDate.now().plusDays(30).toString();
		return """
			{"categoryId":%d,"title":"%s","content":"상세정보입니다.","isContactPublic":%s,"item":"지퍼 파우치",\
			"quantity":%d,"quantityUnit":"매","desiredPrice":6500000,"packageType":"지퍼스탠딩 파우치",\
			"contents":"견과류 100g","spec":"160mm x 230mm","materialThickness":"총 104u","printSpec":"앞면 6도",\
			"materialComposition":"PET 12u","artworkProvided":"제공 가능","existingSample":"제공 가능",\
			"desiredDeliveryDate":"%s","deadlineDate":"%s","deliveryRegion":"경기 파주","deliveryMethod":"1회 일괄 납품",\
			"priceTerms":"부가세 별도","paymentTerms":"협의 가능","fileIds":%s}\
			""".formatted(categoryId, title, isContactPublic, quantity, date, date, fileIds.toString());
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone) {
		return memberRepository.save(Member.signUp(email, "{noop}password", "테스트", phone, MemberType.COMPANY))
			.getMemberId();
	}

	private void saveCompany(Long memberId, String name) {
		companyRepository.save(Company.builder()
			.memberId(memberId)
			.name(name)
			.industry("포장")
			.products("지퍼파우치")
			.businessHours("09:00 - 18:00")
			.contactPhone("02-1234-5678")
			.address("경기 파주시 1")
			.representativeName("홍길동")
			.foundedDate(LocalDate.of(2015, 3, 2))
			.employeeCount(12)
			.description("설명")
			.build());
	}

	private Long saveFile(Long uploaderId, FilePurpose purpose) {
		String contentType = purpose == FilePurpose.POST_IMAGE ? "image/png" : "application/pdf";
		String extension = contentType.substring(contentType.indexOf('/') + 1);
		String storageKey = purpose.getFolder() + "/" + uploaderId + "/" + UUID.randomUUID() + "." + extension;
		return fileRepository.save(File.register(uploaderId, "원본." + extension, storageKey, contentType, 1024))
			.getFileId();
	}
}
