package com.example.packhub.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.packhub.domain.post.entity.AttachType;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostAttachment;
import com.example.packhub.domain.post.entity.PostRequest;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.entity.PostType;
import com.example.packhub.domain.post.entity.PostVisibility;
import com.example.packhub.domain.post.repository.PostAttachmentRepository;
import com.example.packhub.domain.post.repository.PostRepository;
import com.example.packhub.domain.post.repository.PostRequestRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 의뢰 등록 API (5-3).
 * 요청이 실제로 커밋되므로(테스트 롤백 불가) 테스트마다 데이터를 직접 지운다. 분류(Category)는 기초 데이터라 지우지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RequestApiTest {

	/** 기초 데이터: 100 = 대분류 "연포장·파우치", 105 = 그 아래 소분류 "지퍼파우치" */
	private static final long PARENT_CATEGORY = 100L;
	private static final long SUB_CATEGORY = 105L;

	@Autowired
	private MockMvc mockMvc;

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

	/** 업체를 등록한 기업회원 */
	private Long owner;
	private Long ownerCompanyId;
	/** 업체를 아직 등록하지 않은 기업회원 */
	private Long companyWithoutCompany;
	private Long general;

	@BeforeEach
	void setUp() {
		owner = saveMember("rq-owner@test.com", "010-9300-0001", MemberType.COMPANY);
		companyWithoutCompany = saveMember("rq-nocompany@test.com", "010-9300-0002", MemberType.COMPANY);
		general = saveMember("rq-general@test.com", "010-9300-0003", MemberType.GENERAL);
		ownerCompanyId = saveCompany(owner);
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

	@Test
	void 등록하면_공통정보와_의뢰정보와_첨부가_같은_postId로_저장되고_바로_게시된다() throws Exception {
		Long image = saveFile(owner, FilePurpose.POST_IMAGE, "image/png");
		Long document = saveFile(owner, FilePurpose.POST_DOCUMENT, "application/pdf");

		Number postId = JsonPath.read(create(owner, body(SUB_CATEGORY, "[" + document + ", " + image + "]"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("OPEN"))
			.andExpect(jsonPath("$.data.createdAt").exists())
			.andReturn().getResponse().getContentAsString(), "$.data.postId");

		Post post = postRepository.findById(postId.longValue()).orElseThrow();
		assertThat(post.getMemberId()).isEqualTo(owner);
		assertThat(post.getCompanyId()).isEqualTo(ownerCompanyId);
		assertThat(post.getPostType()).isEqualTo(PostType.REQUEST);
		assertThat(post.getCategoryId()).isEqualTo(SUB_CATEGORY);
		assertThat(post.getTitle()).isEqualTo("견과류용 지퍼 스탠딩 파우치 제작의뢰");
		assertThat(post.getStatus()).isEqualTo(PostStatus.OPEN);
		assertThat(post.getVisibility()).isEqualTo(PostVisibility.VISIBLE);
		assertThat(post.isContactPublic()).isTrue();
		assertThat(post.getViewCount()).isZero();
		// 담당자 연락처는 글에 저장하지 않는다 (상세 조회 때 회원정보를 읽는다)
		assertThat(post.getContactName()).isNull();
		assertThat(post.getContactPhone()).isNull();
		assertThat(post.getContactEmail()).isNull();

		PostRequest detail = postRequestRepository.findById(postId.longValue()).orElseThrow();
		assertThat(detail.getItem()).isEqualTo("지퍼 파우치");
		assertThat(detail.getQuantity()).isEqualTo(50000);
		assertThat(detail.getQuantityUnit()).isEqualTo("매");
		assertThat(detail.getDesiredPrice()).isEqualTo(6500000L);
		assertThat(detail.getMaterialComposition()).isEqualTo("PET 12u, VMPET 12u, PE 80u");
		assertThat(detail.getDesiredDeliveryDate()).isEqualTo(LocalDate.now().plusDays(30));
		assertThat(detail.getPaymentTerms()).isEqualTo("협의 가능");

		// 순서는 보낸 순서(문서 → 사진), 종류는 업로드 용도로 정한다
		assertThat(postAttachmentRepository.findAll().stream()
			.sorted(Comparator.comparingInt(PostAttachment::getSortOrder)).toList())
			.extracting(PostAttachment::getPostId, PostAttachment::getFileId, PostAttachment::getAttachType,
				PostAttachment::getSortOrder)
			.containsExactly(
				tuple(postId.longValue(), document, AttachType.DOCUMENT, 1),
				tuple(postId.longValue(), image, AttachType.IMAGE, 2));
	}

	@Test
	void 첨부자료는_보내지_않아도_등록된다() throws Exception {
		create(owner, body(SUB_CATEGORY, null))
			.andExpect(status().isCreated());

		assertThat(postRepository.count()).isEqualTo(1);
		assertThat(postAttachmentRepository.count()).isZero();
	}

	@Test
	void 첨부자료_말고_빈_칸이_있으면_칸별_사유와_함께_400이다() throws Exception {
		String body = body(SUB_CATEGORY, null)
			.replace("\"title\": \"견과류용 지퍼 스탠딩 파우치 제작의뢰\"", "\"title\": \"\"")
			.replace("\"packageType\": \"지퍼스탠딩 파우치\",", "");

		create(owner, body)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[*].field").value(
				org.hamcrest.Matchers.containsInAnyOrder("title", "packageType")));
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 희망납기가_오늘보다_앞이면_400이고_오늘은_된다() throws Exception {
		create(owner, body(SUB_CATEGORY, null)
			.replace(LocalDate.now().plusDays(30).toString(), LocalDate.now().minusDays(1).toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("desiredDeliveryDate"));

		create(owner, body(SUB_CATEGORY, null)
			.replace(LocalDate.now().plusDays(30).toString(), LocalDate.now().toString()))
			.andExpect(status().isCreated());
	}

	@Test
	void 수량이_0이거나_첨부가_10개를_넘으면_400이다() throws Exception {
		create(owner, body(SUB_CATEGORY, null).replace("\"quantity\": 50000", "\"quantity\": 0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("quantity"));

		String elevenIds = LongStream.rangeClosed(1, 11).mapToObj(String::valueOf)
			.collect(Collectors.joining(", ", "[", "]"));
		create(owner, body(SUB_CATEGORY, elevenIds))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("fileIds"));
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(post("/api/v1/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(SUB_CATEGORY, null)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 일반회원이면_403_COMPANY_MEMBER_REQUIRED다() throws Exception {
		create(general, body(SUB_CATEGORY, null))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_MEMBER_REQUIRED"));
	}

	@Test
	void 업체를_등록하지_않은_기업회원이면_403_COMPANY_REGISTRATION_REQUIRED다() throws Exception {
		create(companyWithoutCompany, body(SUB_CATEGORY, null))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_REGISTRATION_REQUIRED"));
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 대분류_카테고리면_400_INVALID_CATEGORY다() throws Exception {
		create(owner, body(PARENT_CATEGORY, null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"));
	}

	@Test
	void 남이_올린_파일이면_403이고_아무것도_저장되지_않는다() throws Exception {
		Long othersFile = saveFile(general, FilePurpose.POST_IMAGE, "image/png");

		create(owner, body(SUB_CATEGORY, "[" + othersFile + "]"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FILE_ACCESS_DENIED"));
		assertThat(postRepository.count()).isZero();
		assertThat(postRequestRepository.count()).isZero();
	}

	@Test
	void 게시글_용도가_아닌_파일이면_400_FILE_PURPOSE_MISMATCH다() throws Exception {
		Long companyImage = saveFile(owner, FilePurpose.COMPANY_IMAGE, "image/png");

		create(owner, body(SUB_CATEGORY, "[" + companyImage + "]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("FILE_PURPOSE_MISMATCH"));
	}

	@Test
	void 없는_파일이면_404_FILE_NOT_FOUND다() throws Exception {
		create(owner, body(SUB_CATEGORY, "[999999]"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
	}

	@Test
	void 같은_파일을_두_번_넣으면_400이다() throws Exception {
		Long image = saveFile(owner, FilePurpose.POST_IMAGE, "image/png");

		create(owner, body(SUB_CATEGORY, "[" + image + ", " + image + "]"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	private ResultActions create(long memberId, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/requests")
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	/** 첨부자료 말고 모든 칸을 채운 요청. fileIds가 null이면 첨부 칸을 보내지 않는다 */
	private String body(long categoryId, String fileIds) {
		String deliveryDate = LocalDate.now().plusDays(30).toString();
		String deadline = LocalDate.now().plusDays(10).toString();
		return """
			{
			  "categoryId": %d,
			  "title": "견과류용 지퍼 스탠딩 파우치 제작의뢰",
			  "content": "견과류 100g용 지퍼 스탠딩 파우치입니다.",
			  "isContactPublic": true,
			  "item": "지퍼 파우치",
			  "quantity": 50000,
			  "quantityUnit": "매",
			  "desiredPrice": 6500000,
			  "packageType": "지퍼스탠딩 파우치",
			  "contents": "견과류 100g",
			  "spec": "160mm x 230mm, 밑지 80mm",
			  "materialThickness": "총 104u, 업체 제안 필요",
			  "printSpec": "앞면 6도 뒷면 6도",
			  "materialComposition": "PET 12u, VMPET 12u, PE 80u",
			  "artworkProvided": "제공 가능",
			  "existingSample": "제공 가능",
			  "desiredDeliveryDate": "%s",
			  "deadlineDate": "%s",
			  "deliveryRegion": "경기 파주",
			  "deliveryMethod": "1회 일괄 납품",
			  "priceTerms": "부가세·운송비 별도",
			  "paymentTerms": "협의 가능"%s
			}
			""".formatted(categoryId, deliveryDate, deadline, fileIds == null ? "" : ",\n  \"fileIds\": " + fileIds);
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone, MemberType memberType) {
		return memberRepository.save(Member.signUp(email, "{noop}password", "테스트", phone, memberType)).getMemberId();
	}

	private Long saveCompany(Long memberId) {
		return companyRepository.save(Company.builder()
			.memberId(memberId)
			.name("팩허브 푸드")
			.industry("그라비아 생산지 포장 업체")
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
