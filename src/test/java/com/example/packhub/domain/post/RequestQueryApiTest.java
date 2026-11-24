package com.example.packhub.domain.post;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.favorite.entity.Favorite;
import com.example.packhub.domain.favorite.repository.FavoriteRepository;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.post.dto.RequestSaveRequest;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 의뢰 목록·상세 조회 API (5-1, 5-2, 5-5).
 * 의뢰는 등록 서비스로 만들고, 마감·숨김·삭제·지난 납기처럼 API로 만들 수 없는 상태는 DB를 직접 고친다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RequestQueryApiTest.FakeS3Config.class})
class RequestQueryApiTest {

	/** 상세의 첨부 조회 주소는 서명 계산만 하므로 AWS 키가 없는 PC에서도 돌도록 가짜 키를 쓴다 */
	@TestConfiguration(proxyBeanMethods = false)
	static class FakeS3Config {

		@Bean
		@Primary
		S3Presigner fakeS3Presigner() {
			return S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
				.build();
		}
	}

	/** 기초 데이터: 100 = 대분류 "연포장·파우치", 101·105 = 그 아래 소분류, 201 = 다른 대분류(200)의 소분류 */
	private static final long PARENT_CATEGORY = 100L;
	private static final long SUB_CATEGORY = 105L;
	private static final long SIBLING_SUB_CATEGORY = 101L;
	private static final long OTHER_PARENT_SUB_CATEGORY = 201L;

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
	private FavoriteRepository favoriteRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** "팩허브 푸드"(업종: 그라비아 생산지 포장 업체)를 등록한 기업회원 */
	private Long owner;
	private Long ownerCompanyId;
	/** "오렌지 패키징"을 등록한 다른 기업회원 */
	private Long otherOwner;
	private Long viewer;

	@BeforeEach
	void setUp() {
		owner = saveMember("rq-q-owner@test.com", "01093100001", MemberType.COMPANY, "최유성");
		otherOwner = saveMember("rq-q-other@test.com", "01093100002", MemberType.COMPANY, "김오렌");
		viewer = saveMember("rq-q-viewer@test.com", "01093100003", MemberType.GENERAL, "보는이");
		ownerCompanyId = saveCompany(owner, "팩허브 푸드", "그라비아 생산지 포장 업체");
		saveCompany(otherOwner, "오렌지 패키징", "종이상자 제조");
	}

	@AfterEach
	void cleanUp() {
		favoriteRepository.deleteAllInBatch();
		postAttachmentRepository.deleteAllInBatch();
		postRequestRepository.deleteAllInBatch();
		postRepository.deleteAllInBatch();
		companyRepository.deleteAllInBatch();
		fileRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 목록_카드에는_업종_제목_업체명_수량_희망납기가_나오고_로그인_없이_볼_수_있다() throws Exception {
		Long postId = create(owner, "견과류 파우치 제작의뢰", SUB_CATEGORY, 30, true, List.of());

		mockMvc.perform(get("/api/v1/requests"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].postId").value(postId))
			.andExpect(jsonPath("$.data.content[0].title").value("견과류 파우치 제작의뢰"))
			.andExpect(jsonPath("$.data.content[0].industry").value("그라비아 생산지 포장 업체"))
			.andExpect(jsonPath("$.data.content[0].companyName").value("팩허브 푸드"))
			.andExpect(jsonPath("$.data.content[0].quantity").value(50000))
			.andExpect(jsonPath("$.data.content[0].quantityUnit").value("매"))
			.andExpect(jsonPath("$.data.content[0].desiredDeliveryDate").value(LocalDate.now().plusDays(30).toString()))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(24))
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void 목록에는_게시_중인_의뢰만_나오고_마감_숨김_삭제된_의뢰는_빠진다() throws Exception {
		Long open = create(owner, "게시 중", SUB_CATEGORY, 30, true, List.of());
		Long closed = create(owner, "마감", SUB_CATEGORY, 30, true, List.of());
		Long hidden = create(owner, "숨김", SUB_CATEGORY, 30, true, List.of());
		Long deleted = create(owner, "삭제", SUB_CATEGORY, 30, true, List.of());
		jdbcTemplate.update("UPDATE Post SET status = 'CLOSED', closedAt = NOW() WHERE postId = ?", closed);
		jdbcTemplate.update("UPDATE Post SET visibility = 'HIDDEN' WHERE postId = ?", hidden);
		jdbcTemplate.update("UPDATE Post SET deletedAt = NOW() WHERE postId = ?", deleted);

		mockMvc.perform(get("/api/v1/requests"))
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(open.intValue())))
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void 최신순은_나중에_등록한_의뢰가_먼저다() throws Exception {
		Long first = create(owner, "첫째", SUB_CATEGORY, 30, true, List.of());
		Long second = create(owner, "둘째", SUB_CATEGORY, 10, true, List.of());
		Long third = create(owner, "셋째", SUB_CATEGORY, 20, true, List.of());

		mockMvc.perform(get("/api/v1/requests"))
			.andExpect(jsonPath("$.data.content[*].postId")
				.value(contains(third.intValue(), second.intValue(), first.intValue())));
	}

	@Test
	void 납기_정렬은_희망납기_기준이고_희망납기가_지난_의뢰는_맨_뒤다() throws Exception {
		Long far = create(owner, "먼 납기", SUB_CATEGORY, 30, true, List.of());
		Long near = create(owner, "가까운 납기", SUB_CATEGORY, 2, true, List.of());
		Long past = create(owner, "지난 납기", SUB_CATEGORY, 5, true, List.of());
		Long today = create(owner, "오늘 납기", SUB_CATEGORY, 0, true, List.of());
		jdbcTemplate.update("UPDATE PostRequest SET desiredDeliveryDate = ? WHERE postId = ?",
			LocalDate.now().minusDays(3), past);

		mockMvc.perform(get("/api/v1/requests").param("sort", "DELIVERY_ASC"))
			.andExpect(jsonPath("$.data.content[*].postId")
				.value(contains(today.intValue(), near.intValue(), far.intValue(), past.intValue())));
		mockMvc.perform(get("/api/v1/requests").param("sort", "DELIVERY_DESC"))
			.andExpect(jsonPath("$.data.content[*].postId")
				.value(contains(far.intValue(), near.intValue(), today.intValue(), past.intValue())));
	}

	@Test
	void 검색어는_의뢰_제목과_업체명에서_찾는다() throws Exception {
		Long byTitle = create(owner, "견과류 파우치", SUB_CATEGORY, 30, true, List.of());
		Long byCompany = create(otherOwner, "택배 박스", SUB_CATEGORY, 30, true, List.of());
		create(owner, "쇼핑백", SUB_CATEGORY, 30, true, List.of());

		mockMvc.perform(get("/api/v1/requests").param("keyword", "파우치"))
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(byTitle.intValue())));
		mockMvc.perform(get("/api/v1/requests").param("keyword", "오렌지"))
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(byCompany.intValue())));
		// %, _는 글자 그대로 찾는다
		mockMvc.perform(get("/api/v1/requests").param("keyword", "%"))
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 대분류로_찾으면_그_아래_소분류_의뢰가_모두_나온다() throws Exception {
		Long sub = create(owner, "지퍼파우치", SUB_CATEGORY, 30, true, List.of());
		Long sibling = create(owner, "삼방봉투", SIBLING_SUB_CATEGORY, 30, true, List.of());
		create(owner, "다른 대분류", OTHER_PARENT_SUB_CATEGORY, 30, true, List.of());

		mockMvc.perform(get("/api/v1/requests").param("categoryId", String.valueOf(PARENT_CATEGORY)))
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(sibling.intValue(), sub.intValue())));
		mockMvc.perform(get("/api/v1/requests").param("categoryId", String.valueOf(SUB_CATEGORY)))
			.andExpect(jsonPath("$.data.content[*].postId").value(contains(sub.intValue())));
	}

	@Test
	void 페이지를_나눠서_돌려준다() throws Exception {
		for (int i = 0; i < 3; i++) {
			create(owner, "의뢰" + i, SUB_CATEGORY, 30, true, List.of());
		}

		mockMvc.perform(get("/api/v1/requests").param("page", "2").param("size", "2"))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.page").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(2));
		mockMvc.perform(get("/api/v1/requests").param("page", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 상세에는_의뢰_칸과_작성_업체와_의뢰자_정보와_첨부가_순서대로_나온다() throws Exception {
		Long document = saveFile(owner, FilePurpose.POST_DOCUMENT, "application/pdf");
		Long image = saveFile(owner, FilePurpose.POST_IMAGE, "image/png");
		Long postId = create(owner, "견과류 파우치 제작의뢰", SUB_CATEGORY, 30, true, List.of(document, image));

		mockMvc.perform(get("/api/v1/requests/{postId}", postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.postId").value(postId))
			.andExpect(jsonPath("$.data.title").value("견과류 파우치 제작의뢰"))
			.andExpect(jsonPath("$.data.status").value("OPEN"))
			.andExpect(jsonPath("$.data.categoryId").value(SUB_CATEGORY))
			.andExpect(jsonPath("$.data.isMine").value(false))
			.andExpect(jsonPath("$.data.writer.companyId").value(ownerCompanyId))
			.andExpect(jsonPath("$.data.writer.name").value("팩허브 푸드"))
			.andExpect(jsonPath("$.data.writer.industry").value("그라비아 생산지 포장 업체"))
			.andExpect(jsonPath("$.data.contact.isPublic").value(true))
			.andExpect(jsonPath("$.data.contact.name").value("최유성"))
			.andExpect(jsonPath("$.data.contact.phone").value("01093100001"))
			.andExpect(jsonPath("$.data.contact.email").value("rq-q-owner@test.com"))
			.andExpect(jsonPath("$.data.contact.hours").value(nullValue()))
			.andExpect(jsonPath("$.data.item").value("지퍼 파우치"))
			.andExpect(jsonPath("$.data.desiredPrice").value(6500000))
			.andExpect(jsonPath("$.data.paymentTerms").value("협의 가능"))
			.andExpect(jsonPath("$.data.attachments[*].fileId").value(contains(document.intValue(), image.intValue())))
			.andExpect(jsonPath("$.data.attachments[*].attachType").value(contains("DOCUMENT", "IMAGE")))
			.andExpect(jsonPath("$.data.attachments[0].sizeBytes").value(1024))
			.andExpect(jsonPath("$.data.attachments[0].url").value(containsString("post-document")));
	}

	@Test
	void 작성자가_보면_isMine이_true다() throws Exception {
		Long postId = create(owner, "내 의뢰", SUB_CATEGORY, 30, true, List.of());

		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
			.andExpect(jsonPath("$.data.isMine").value(true));
		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(viewer)))
			.andExpect(jsonPath("$.data.isMine").value(false));
	}

	@Test
	void 개인정보_공개를_끈_의뢰는_남에게는_의뢰자_정보를_숨기고_작성자에게는_보여_준다() throws Exception {
		Long postId = create(owner, "비공개 의뢰", SUB_CATEGORY, 30, false, List.of());

		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(viewer)))
			.andExpect(jsonPath("$.data.contact.isPublic").value(false))
			.andExpect(jsonPath("$.data.contact.name").value(nullValue()))
			.andExpect(jsonPath("$.data.contact.phone").value(nullValue()))
			.andExpect(jsonPath("$.data.contact.email").value(nullValue()));
		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
			.andExpect(jsonPath("$.data.contact.isPublic").value(false))
			.andExpect(jsonPath("$.data.contact.phone").value("01093100001"));
	}

	@Test
	void 회원정보를_바꾸면_이미_올린_의뢰의_의뢰자_정보도_바뀐다() throws Exception {
		Long postId = create(owner, "의뢰", SUB_CATEGORY, 30, true, List.of());
		jdbcTemplate.update("UPDATE Member SET phone = '01055556666' WHERE memberId = ?", owner);

		mockMvc.perform(get("/api/v1/requests/{postId}", postId))
			.andExpect(jsonPath("$.data.contact.phone").value("01055556666"));
	}

	@Test
	void 목록은_로그인한_회원이_관심으로_저장한_의뢰만_favorited가_true다() throws Exception {
		Long saved = create(owner, "저장한 의뢰", SUB_CATEGORY, 30, true, List.of());
		Long notSaved = create(owner, "저장하지 않은 의뢰", SUB_CATEGORY, 30, true, List.of());
		favoriteRepository.save(Favorite.ofPost(viewer, saved));
		favoriteRepository.save(Favorite.ofPost(owner, notSaved));

		mockMvc.perform(get("/api/v1/requests").header(HttpHeaders.AUTHORIZATION, bearer(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[?(@.postId == %d)].favorited".formatted(saved)).value(contains(true)))
			.andExpect(
				jsonPath("$.data.content[?(@.postId == %d)].favorited".formatted(notSaved)).value(contains(false)));
		mockMvc.perform(get("/api/v1/requests"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].favorited").value(contains(false, false)));
	}

	@Test
	void 상세는_로그인한_회원이_관심으로_저장했으면_favorited가_true다() throws Exception {
		Long postId = create(owner, "저장한 의뢰", SUB_CATEGORY, 30, true, List.of());
		favoriteRepository.save(Favorite.ofPost(viewer, postId));

		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(viewer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(true));
		mockMvc.perform(get("/api/v1/requests/{postId}", postId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(false));
		mockMvc.perform(get("/api/v1/requests/{postId}", postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(false));
	}

	@Test
	void 마감된_의뢰도_상세는_볼_수_있다() throws Exception {
		Long postId = create(owner, "마감 의뢰", SUB_CATEGORY, 30, true, List.of());
		jdbcTemplate.update("UPDATE Post SET status = 'CLOSED', closedAt = NOW() WHERE postId = ?", postId);

		mockMvc.perform(get("/api/v1/requests/{postId}", postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("CLOSED"))
			.andExpect(jsonPath("$.data.closedAt").exists());
	}

	@Test
	void 없거나_숨김_삭제된_의뢰는_404다() throws Exception {
		Long hidden = create(owner, "숨김", SUB_CATEGORY, 30, true, List.of());
		Long deleted = create(owner, "삭제", SUB_CATEGORY, 30, true, List.of());
		jdbcTemplate.update("UPDATE Post SET visibility = 'HIDDEN' WHERE postId = ?", hidden);
		jdbcTemplate.update("UPDATE Post SET deletedAt = NOW() WHERE postId = ?", deleted);

		for (Long postId : List.of(hidden, deleted, 999999L)) {
			mockMvc.perform(get("/api/v1/requests/{postId}", postId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
		}
	}

	/** 첨부자료 말고 모든 칸을 채워 등록한다. 희망납기는 오늘에서 deliveryDays일 뒤 */
	private Long create(Long memberId, String title, long categoryId, int deliveryDays, boolean isContactPublic,
		List<Long> fileIds) {
		LocalDate deliveryDate = LocalDate.now().plusDays(deliveryDays);
		return requestService.create(memberId, new RequestSaveRequest(categoryId, title, "상세정보입니다.",
			isContactPublic, "지퍼 파우치", 50000, "매", 6500000L, "지퍼스탠딩 파우치", "견과류 100g",
			"160mm x 230mm, 밑지 80mm", "총 104u", "앞면 6도 뒷면 6도", "PET 12u, VMPET 12u, PE 80u", "제공 가능",
			"제공 가능", deliveryDate, deliveryDate, "경기 파주", "1회 일괄 납품", "부가세·운송비 별도", "협의 가능",
			fileIds)).postId();
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone, MemberType memberType, String name) {
		return memberRepository.save(Member.signUp(email, "{noop}password", name, phone, memberType)).getMemberId();
	}

	private Long saveCompany(Long memberId, String name, String industry) {
		return companyRepository.save(Company.builder()
			.memberId(memberId)
			.name(name)
			.industry(industry)
			.products("지퍼파우치")
			.businessHours("09:00 - 18:00")
			.contactPhone("02-1234-5678")
			.address("경기 파주시 1")
			.representativeName("홍길동")
			.foundedDate(LocalDate.of(2015, 3, 2))
			.employeeCount(12)
			.description("설명")
			.build()).getCompanyId();
	}

	private Long saveFile(Long uploaderId, FilePurpose purpose, String contentType) {
		String extension = contentType.substring(contentType.indexOf('/') + 1);
		String storageKey = purpose.getFolder() + "/" + uploaderId + "/" + UUID.randomUUID() + "." + extension;
		return fileRepository.save(File.register(uploaderId, "원본." + extension, storageKey, contentType, 1024))
			.getFileId();
	}
}
