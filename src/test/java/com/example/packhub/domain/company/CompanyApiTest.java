package com.example.packhub.domain.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.business.entity.BusinessVerification;
import com.example.packhub.domain.business.repository.BusinessVerificationRepository;
import com.example.packhub.domain.company.entity.CertificationType;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyCategory;
import com.example.packhub.domain.company.entity.CompanyCertification;
import com.example.packhub.domain.company.entity.CompanyEquipment;
import com.example.packhub.domain.company.entity.CompanyImage;
import com.example.packhub.domain.company.entity.CompanyImageType;
import com.example.packhub.domain.company.entity.CompanyVisibility;
import com.example.packhub.domain.company.repository.CompanyCategoryRepository;
import com.example.packhub.domain.company.repository.CompanyCertificationRepository;
import com.example.packhub.domain.company.repository.CompanyEquipmentRepository;
import com.example.packhub.domain.company.repository.CompanyImageRepository;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.subscription.entity.BillingCycle;
import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.repository.SubscriptionRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 업체 등록·상세 조회·내 업체 조회·수정·목록 조회, 분류 목록, 보유 인증 목록 API.
 * 요청이 실제로 커밋되므로(테스트 롤백 불가) 테스트마다 데이터를 직접 지운다. 분류(Category)는 기초 데이터라 지우지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CompanyApiTest.FakeS3Config.class})
class CompanyApiTest {

	/**
	 * 상세 조회는 파일마다 조회용 URL에 서명한다. 서명은 S3에 접속하지 않고 계산만 하므로,
	 * AWS 접근 키(~/.aws/credentials)가 없는 PC에서도 테스트가 돌도록 가짜 키를 쓴다.
	 */
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

	/** 기초 데이터: 100 = 대분류 "연포장·파우치", 101 = 그 아래 소분류 "삼방봉투" */
	private static final long PARENT_CATEGORY = 100L;
	private static final long SUB_CATEGORY = 101L;
	private static final long OTHER_SUB_CATEGORY = 102L;
	/** 다른 대분류(200) 아래의 소분류 */
	private static final long OTHER_PARENT_SUB_CATEGORY = 201L;
	private static final long BASIC_PLAN = 1L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private CompanyCategoryRepository companyCategoryRepository;

	@Autowired
	private CompanyCertificationRepository companyCertificationRepository;

	@Autowired
	private CompanyEquipmentRepository companyEquipmentRepository;

	@Autowired
	private CompanyImageRepository companyImageRepository;

	@Autowired
	private FileRepository fileRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BusinessVerificationRepository businessVerificationRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** saveCompany로 만드는 회원의 이메일·전화번호가 겹치지 않게 하는 번호 */
	private int memberSequence;

	/** 이용권을 이용 중인 기업회원 */
	private Long company;
	/** 이용권이 없는 기업회원 */
	private Long companyWithoutPlan;
	private Long general;

	private Long logo;
	private Long background;
	private Long equipmentImage1;
	private Long equipmentImage2;
	private Long portfolioPdf;
	private Long portfolioImage;
	private Long officeImage;

	@BeforeEach
	void setUp() {
		company = saveMember("co-company@test.com", "010-9100-0001", MemberType.COMPANY);
		companyWithoutPlan = saveMember("co-company2@test.com", "010-9100-0002", MemberType.COMPANY);
		general = saveMember("co-general@test.com", "010-9100-0003", MemberType.GENERAL);
		subscriptionRepository.save(Subscription.startPaid(company, BASIC_PLAN, BillingCycle.MONTHLY, LocalDate.now()));

		logo = saveFile(company, FilePurpose.COMPANY_LOGO, "image/png");
		background = saveFile(company, FilePurpose.COMPANY_BACKGROUND, "image/png");
		equipmentImage1 = saveFile(company, FilePurpose.COMPANY_EQUIPMENT, "image/png");
		equipmentImage2 = saveFile(company, FilePurpose.COMPANY_EQUIPMENT, "image/jpeg");
		portfolioPdf = saveFile(company, FilePurpose.COMPANY_PORTFOLIO, "application/pdf");
		portfolioImage = saveFile(company, FilePurpose.COMPANY_PORTFOLIO, "image/png");
		officeImage = saveFile(company, FilePurpose.COMPANY_IMAGE, "image/png");
	}

	@AfterEach
	void cleanUp() {
		companyImageRepository.deleteAllInBatch();
		companyEquipmentRepository.deleteAllInBatch();
		companyCertificationRepository.deleteAllInBatch();
		companyCategoryRepository.deleteAllInBatch();
		companyRepository.deleteAllInBatch();
		fileRepository.deleteAllInBatch();
		subscriptionRepository.deleteAllInBatch();
		businessVerificationRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 등록하면_업체와_카테고리_인증_설비_사진이_요청_순서대로_저장된다() throws Exception {
		Number companyId = JsonPath.read(register(company, fullBody())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andReturn().getResponse().getContentAsString(), "$.data.companyId");

		Company saved = companyRepository.findById(companyId.longValue()).orElseThrow();
		assertThat(saved.getMemberId()).isEqualTo(company);
		assertThat(saved.getName()).isEqualTo("팩허브 팩토리");
		assertThat(saved.getIndustry()).isEqualTo("그라비아 연포장");
		assertThat(saved.getProducts()).isEqualTo("스탠딩파우치, 지퍼파우치");
		assertThat(saved.getFoundedDate()).isEqualTo(LocalDate.of(2015, 3, 2));
		assertThat(saved.getEmployeeCount()).isEqualTo(12);
		assertThat(saved.getHomepageUrl()).isNull();
		assertThat(saved.getLogoFileId()).isEqualTo(logo);
		assertThat(saved.getBackgroundFileId()).isEqualTo(background);
		assertThat(saved.getViewCount()).isZero();
		assertThat(saved.getVisibility()).isEqualTo(CompanyVisibility.VISIBLE);
		assertThat(saved.getDeletedAt()).isNull();

		assertThat(companyCategoryRepository.findAll())
			.extracting(CompanyCategory::getCompanyId, CompanyCategory::getCategoryId)
			.containsExactly(tuple(saved.getCompanyId(), SUB_CATEGORY));

		// 요청은 HACCP, ISO_9001, HACCP. 중복은 한 번만 저장한다
		assertThat(sorted(companyCertificationRepository.findAll(), CompanyCertification::getSortOrder))
			.extracting(CompanyCertification::getCertType, CompanyCertification::getSortOrder,
				CompanyCertification::getImageFileId)
			.containsExactly(tuple(CertificationType.HACCP, 1, null), tuple(CertificationType.ISO_9001, 2, null));

		assertThat(sorted(companyEquipmentRepository.findAll(), CompanyEquipment::getSortOrder))
			.extracting(CompanyEquipment::getName, CompanyEquipment::getImageFileId, CompanyEquipment::getSortOrder)
			.containsExactly(tuple("그라비아 인쇄기", equipmentImage1, 1), tuple("드라이 라미네이터", equipmentImage2, 2));

		assertThat(sorted(imagesOf(CompanyImageType.PORTFOLIO), CompanyImage::getSortOrder))
			.extracting(CompanyImage::getFileId, CompanyImage::getSortOrder)
			.containsExactly(tuple(portfolioPdf, 1), tuple(portfolioImage, 2));
		assertThat(imagesOf(CompanyImageType.OFFICE))
			.extracting(CompanyImage::getFileId, CompanyImage::getSortOrder)
			.containsExactly(tuple(officeImage, 1));
	}

	@Test
	void 선택_항목을_보내지_않아도_등록된다() throws Exception {
		register(company, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());

		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyCategoryRepository.count()).isEqualTo(1);
		assertThat(companyCertificationRepository.count()).isZero();
		assertThat(companyEquipmentRepository.count()).isZero();
		assertThat(companyImageRepository.count()).isZero();
	}

	@Test
	void 일반회원은_등록할_수_없다() throws Exception {
		register(general, minimalBody(SUB_CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_MEMBER_REQUIRED"));
	}

	@Test
	void 이용권이_없는_기업회원은_등록할_수_없다() throws Exception {
		register(companyWithoutPlan, minimalBody(SUB_CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_REQUIRED"));
	}

	@Test
	void 이미_등록한_업체가_있으면_409() throws Exception {
		register(company, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());

		register(company, minimalBody(SUB_CATEGORY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("COMPANY_ALREADY_EXISTS"));
		assertThat(companyRepository.count()).isEqualTo(1);
	}

	@Test
	void 같은_회원이_동시에_두_번_등록해도_업체는_하나만_생긴다() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> request = () -> {
			start.await();
			return register(company, minimalBody(SUB_CATEGORY)).andReturn().getResponse().getStatus();
		};
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> first = executor.submit(request);
			Future<Integer> second = executor.submit(request);
			start.countDown();

			assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
		} finally {
			executor.shutdownNow();
		}
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyCategoryRepository.count()).isEqualTo(1);
	}

	@Test
	void 코드의_확인을_거치지_않아도_DB가_회원당_업체_1개를_막는다() throws Exception {
		register(company, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());

		// 등록된 업체 행을 그대로 한 번 더 넣는다. 서비스는 이 제약 이름을 보고 409로 바꾼다
		assertThatThrownBy(() -> jdbcTemplate.update("""
			INSERT INTO `Company` (`memberId`, `name`, `industry`, `products`, `businessHours`, `contactPhone`,
				`address`, `representativeName`, `foundedDate`, `employeeCount`, `description`, `visibility`)
			SELECT `memberId`, `name`, `industry`, `products`, `businessHours`, `contactPhone`,
				`address`, `representativeName`, `foundedDate`, `employeeCount`, `description`, `visibility`
			FROM `Company` WHERE `memberId` = ?
			""", company))
			.isInstanceOf(DuplicateKeyException.class)
			.hasMessageContaining("UK_COMPANY_ACTIVE_MEMBER");
	}

	@Test
	void 업체를_삭제한_뒤에는_다시_등록할_수_있다() throws Exception {
		register(company, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `memberId` = ?", company);

		register(company, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());
		assertThat(companyRepository.count()).isEqualTo(2);
	}

	@Test
	void 사업자_인증을_하지_않은_기업회원은_이용권이_있어도_등록할_수_없다() throws Exception {
		Long unverified = saveUnverifiedMember("co-unverified@test.com", "010-9100-0006", MemberType.COMPANY);
		subscriptionRepository.save(Subscription.startPaid(unverified, BASIC_PLAN, BillingCycle.MONTHLY, LocalDate.now()));

		register(unverified, minimalBody(SUB_CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("BUSINESS_NOT_VERIFIED"));
		assertThat(companyRepository.count()).isZero();
	}

	@Test
	void 기간이_끝난_이용권으로는_등록할_수_없다() throws Exception {
		Long expired = saveMember("co-expired@test.com", "010-9100-0004", MemberType.COMPANY);
		subscriptionRepository.save(
			Subscription.startPaid(expired, BASIC_PLAN, BillingCycle.MONTHLY, LocalDate.now().minusMonths(2)));

		register(expired, minimalBody(SUB_CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_REQUIRED"));
	}

	@Test
	void 이용권_종료일_당일까지는_등록할_수_있다() throws Exception {
		Long lastDay = saveMember("co-lastday@test.com", "010-9100-0005", MemberType.COMPANY);
		Long subscriptionId = subscriptionRepository.save(
			Subscription.startPaid(lastDay, BASIC_PLAN, BillingCycle.MONTHLY, LocalDate.now().minusMonths(1)))
			.getSubscriptionId();
		// 시작일로 계산하면 월말 근처에서 종료일이 어긋나므로, 종료일을 오늘로 직접 맞춘다
		LocalDate today = LocalDate.now();
		jdbcTemplate.update("UPDATE `Subscription` SET `currentPeriodEnd` = ? WHERE `subscriptionId` = ?",
			today, subscriptionId);
		assertThat(subscriptionRepository.findById(subscriptionId).orElseThrow().getCurrentPeriodEnd())
			.isEqualTo(today);

		register(lastDay, minimalBody(SUB_CATEGORY)).andExpect(status().isCreated());
	}

	@Test
	void 대분류는_카테고리로_고를_수_없다() throws Exception {
		register(company, minimalBody(PARENT_CATEGORY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"));
	}

	@Test
	void 없는_카테고리는_400() throws Exception {
		register(company, minimalBody(999_999L))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"));
	}

	@Test
	void 목록에_없는_인증_코드는_400() throws Exception {
		register(company, fullBody().replace("\"ISO_9001\"", "\"NOT_A_CERT\""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 설비에_사진이_없으면_400() throws Exception {
		register(company, fullBody().replace(",\"imageFileId\":" + equipmentImage2, ""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 설비는_20개까지만_받는다() throws Exception {
		String tooMany = IntStream.rangeClosed(1, 21)
			.mapToObj(i -> "{\"name\":\"설비 %d\",\"imageFileId\":%d}".formatted(i, equipmentImage1))
			.collect(Collectors.joining(",", "\"equipments\":[", "]"));

		register(company, minimalBody(SUB_CATEGORY).replace("\"employeeCount\":12", "\"employeeCount\":12," + tooMany))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 직원_수는_필수다() throws Exception {
		register(company, minimalBody(SUB_CATEGORY).replace(",\"employeeCount\":12", ""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("employeeCount"));
	}

	@Test
	void 남이_올린_파일은_쓸_수_없다() throws Exception {
		Long othersLogo = saveFile(companyWithoutPlan, FilePurpose.COMPANY_LOGO, "image/png");

		register(company, fullBody().replace("\"logoFileId\":" + logo, "\"logoFileId\":" + othersLogo))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FILE_ACCESS_DENIED"));
	}

	@Test
	void 다른_용도로_올린_파일은_쓸_수_없다() throws Exception {
		// 업체 사진 자리에 로고용으로 올린 파일을 넣는다
		register(company, fullBody().replace("\"officeFileIds\":[" + officeImage, "\"officeFileIds\":[" + logo))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("FILE_PURPOSE_MISMATCH"));
	}

	@Test
	void 없는_파일이면_404이고_아무것도_저장하지_않는다() throws Exception {
		register(company, fullBody().replace("\"backgroundFileId\":" + background, "\"backgroundFileId\":999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));

		assertThat(companyRepository.count()).isZero();
		assertThat(companyCategoryRepository.count()).isZero();
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/companies")
				.contentType(MediaType.APPLICATION_JSON)
				.content(minimalBody(SUB_CATEGORY)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 상세_조회는_로그인_없이_업체_정보와_목록을_등록한_순서대로_반환한다() throws Exception {
		long companyId = registerAndGetId(fullBody());

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.companyId").value(companyId))
			.andExpect(jsonPath("$.data.name").value("팩허브 팩토리"))
			.andExpect(jsonPath("$.data.industry").value("그라비아 연포장"))
			.andExpect(jsonPath("$.data.products").value("스탠딩파우치, 지퍼파우치"))
			.andExpect(jsonPath("$.data.category.categoryId").value(101))
			.andExpect(jsonPath("$.data.category.name").value("삼방봉투"))
			.andExpect(jsonPath("$.data.category.parentId").value(100))
			.andExpect(jsonPath("$.data.category.parentName").value("연포장·파우치"))
			.andExpect(jsonPath("$.data.addressDetail").value("2층"))
			.andExpect(jsonPath("$.data.foundedDate").value("2015-03-02"))
			.andExpect(jsonPath("$.data.employeeCount").value(12))
			.andExpect(jsonPath("$.data.viewCount").doesNotExist())
			.andExpect(jsonPath("$.data.logo.fileId").value(logo))
			.andExpect(jsonPath("$.data.logo.url").value(containsString("company-logo/" + company + "/")))
			.andExpect(jsonPath("$.data.logo.contentType").value("image/png"))
			.andExpect(jsonPath("$.data.background.fileId").value(background))
			.andExpect(jsonPath("$.data.certifications.length()").value(2))
			.andExpect(jsonPath("$.data.certifications[0].code").value("HACCP"))
			.andExpect(jsonPath("$.data.certifications[0].label").value("식품안전 인증(HACCP)"))
			.andExpect(jsonPath("$.data.certifications[1].code").value("ISO_9001"))
			.andExpect(jsonPath("$.data.equipments.length()").value(2))
			.andExpect(jsonPath("$.data.equipments[0].name").value("그라비아 인쇄기"))
			.andExpect(jsonPath("$.data.equipments[0].image.fileId").value(equipmentImage1))
			.andExpect(jsonPath("$.data.equipments[0].image.url").value(containsString("company-equipment/")))
			.andExpect(jsonPath("$.data.equipments[1].name").value("드라이 라미네이터"))
			.andExpect(jsonPath("$.data.portfolios.length()").value(2))
			.andExpect(jsonPath("$.data.portfolios[0].fileId").value(portfolioPdf))
			.andExpect(jsonPath("$.data.portfolios[0].contentType").value("application/pdf"))
			.andExpect(jsonPath("$.data.portfolios[0].originalName").value("원본.pdf"))
			.andExpect(jsonPath("$.data.portfolios[1].contentType").value("image/png"))
			.andExpect(jsonPath("$.data.officeImages.length()").value(1))
			.andExpect(jsonPath("$.data.officeImages[0].fileId").value(officeImage));
	}

	@Test
	void 선택_항목_없이_등록한_업체는_파일이_없고_목록이_비어_있다() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("팩허브 팩토리"))
			.andExpect(jsonPath("$.data.logo").value((Object) null))
			.andExpect(jsonPath("$.data.background").value((Object) null))
			.andExpect(jsonPath("$.data.homepageUrl").value((Object) null))
			.andExpect(jsonPath("$.data.certifications.length()").value(0))
			.andExpect(jsonPath("$.data.equipments.length()").value(0))
			.andExpect(jsonPath("$.data.portfolios.length()").value(0))
			.andExpect(jsonPath("$.data.officeImages.length()").value(0));
	}

	@Test
	void 상세_조회를_해도_조회수는_오르지_않는다() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId)).andExpect(status().isOk());

		assertThat(companyRepository.findById(companyId).orElseThrow().getViewCount()).isZero();
	}

	@Test
	void 없는_업체는_404() throws Exception {
		mockMvc.perform(get("/api/v1/companies/{companyId}", 999_999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 숨김_상태인_업체는_404() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", companyId);

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 삭제한_업체는_404() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `companyId` = ?", companyId);

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 업체_ID가_숫자가_아니면_400() throws Exception {
		mockMvc.perform(get("/api/v1/companies/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 내_업체_조회는_로그인한_기업회원의_업체를_반환한다() throws Exception {
		long companyId = registerAndGetId(fullBody());

		mockMvc.perform(get("/api/v1/companies/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.companyId").value(companyId))
			.andExpect(jsonPath("$.data.name").value("팩허브 팩토리"))
			.andExpect(jsonPath("$.data.certifications.length()").value(2))
			.andExpect(jsonPath("$.data.portfolios.length()").value(2));
	}

	@Test
	void 내_업체_조회는_숨김_상태여도_보인다() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", companyId);

		mockMvc.perform(get("/api/v1/companies/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.companyId").value(companyId));
	}

	@Test
	void 내_업체_조회는_등록한_업체가_없으면_404() throws Exception {
		mockMvc.perform(get("/api/v1/companies/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 내_업체_조회는_삭제한_업체를_반환하지_않는다() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `companyId` = ?", companyId);

		mockMvc.perform(get("/api/v1/companies/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 내_업체_조회는_토큰이_없으면_401이고_일반회원은_403() throws Exception {
		mockMvc.perform(get("/api/v1/companies/me"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/companies/me").header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_MEMBER_REQUIRED"));
	}

	@Test
	void 수정하면_받은_값으로_전체가_바뀌고_뺀_항목은_지워진다() throws Exception {
		long companyId = registerAndGetId(fullBody());

		update(company, updatedBody())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.companyId").value(companyId))
			.andExpect(jsonPath("$.data.name").value("팩허브 패키징"))
			.andExpect(jsonPath("$.data.category.categoryId").value(OTHER_SUB_CATEGORY))
			.andExpect(jsonPath("$.data.addressDetail").doesNotExist())
			.andExpect(jsonPath("$.data.employeeCount").value(30))
			.andExpect(jsonPath("$.data.homepageUrl").value("https://packhub.example.com"))
			.andExpect(jsonPath("$.data.logo").doesNotExist())
			.andExpect(jsonPath("$.data.background.fileId").value(background))
			.andExpect(jsonPath("$.data.certifications.length()").value(2))
			.andExpect(jsonPath("$.data.certifications[0].code").value("ISO_9001"))
			.andExpect(jsonPath("$.data.certifications[1].code").value("KS"))
			.andExpect(jsonPath("$.data.equipments.length()").value(1))
			.andExpect(jsonPath("$.data.equipments[0].name").value("드라이 라미네이터"))
			.andExpect(jsonPath("$.data.equipments[0].image.fileId").value(equipmentImage2))
			.andExpect(jsonPath("$.data.portfolios.length()").value(1))
			.andExpect(jsonPath("$.data.portfolios[0].fileId").value(portfolioImage))
			.andExpect(jsonPath("$.data.officeImages.length()").value(0));

		Company saved = companyRepository.findById(companyId).orElseThrow();
		assertThat(saved.getName()).isEqualTo("팩허브 패키징");
		assertThat(saved.getMemberId()).isEqualTo(company);
		assertThat(saved.getLogoFileId()).isNull();
		assertThat(saved.getVisibility()).isEqualTo(CompanyVisibility.VISIBLE);
		assertThat(companyRepository.count()).isEqualTo(1);
		assertThat(companyCategoryRepository.findAll()).extracting(CompanyCategory::getCategoryId)
			.containsExactly(OTHER_SUB_CATEGORY);
		assertThat(sorted(companyCertificationRepository.findAll(), CompanyCertification::getSortOrder))
			.extracting(CompanyCertification::getCertType)
			.containsExactly(CertificationType.ISO_9001, CertificationType.KS);
		assertThat(companyEquipmentRepository.count()).isEqualTo(1);
		assertThat(imagesOf(CompanyImageType.PORTFOLIO)).extracting(CompanyImage::getFileId)
			.containsExactly(portfolioImage);
		assertThat(imagesOf(CompanyImageType.OFFICE)).isEmpty();
	}

	@Test
	void 같은_값으로_다시_저장해도_수정된다() throws Exception {
		registerAndGetId(fullBody());

		// 인증·사진을 그대로 다시 보내도 (companyId, certType) 중복에 걸리지 않아야 한다
		update(company, fullBody())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.certifications.length()").value(2))
			.andExpect(jsonPath("$.data.equipments.length()").value(2));

		assertThat(companyCertificationRepository.count()).isEqualTo(2);
		assertThat(companyImageRepository.count()).isEqualTo(3);
	}

	@Test
	void 수정_요청이_동시에_와도_기본_정보와_목록이_같은_요청의_값으로_남는다() throws Exception {
		long companyId = registerAndGetId(fullBody());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			for (int round = 0; round < 5; round++) {
				CountDownLatch start = new CountDownLatch(1);
				Callable<Integer> shrink = () -> {
					start.await();
					return update(company, updatedBody()).andReturn().getResponse().getStatus();
				};
				Callable<Integer> restore = () -> {
					start.await();
					return update(company, fullBody()).andReturn().getResponse().getStatus();
				};
				Future<Integer> first = executor.submit(shrink);
				Future<Integer> second = executor.submit(restore);
				start.countDown();
				assertThat(List.of(first.get(), second.get())).containsExactly(200, 200);

				// updatedBody는 이름 "팩허브 패키징"·인증 2개·설비 1개·사진 1개, fullBody는 "팩허브 팩토리"·인증 2개·설비 2개·사진 3개
				boolean shrunk = companyRepository.findById(companyId).orElseThrow().getName().equals("팩허브 패키징");
				assertThat(companyCategoryRepository.count()).isEqualTo(1);
				assertThat(companyCertificationRepository.count()).isEqualTo(2);
				assertThat(companyEquipmentRepository.count()).isEqualTo(shrunk ? 1 : 2);
				assertThat(companyImageRepository.count()).isEqualTo(shrunk ? 1 : 3);
			}
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void 수정은_이용권이_끝나도_할_수_있다() throws Exception {
		registerAndGetId(minimalBody(SUB_CATEGORY));
		subscriptionRepository.deleteAllInBatch();

		update(company, updatedBody()).andExpect(status().isOk());
	}

	@Test
	void 숨김_상태인_업체도_수정할_수_있고_숨김_상태는_그대로다() throws Exception {
		long companyId = registerAndGetId(minimalBody(SUB_CATEGORY));
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", companyId);

		update(company, updatedBody()).andExpect(status().isOk());

		assertThat(companyRepository.findById(companyId).orElseThrow().getVisibility())
			.isEqualTo(CompanyVisibility.HIDDEN);
	}

	@Test
	void 등록한_업체가_없으면_수정은_404() throws Exception {
		update(companyWithoutPlan, minimalBody(SUB_CATEGORY))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
	}

	@Test
	void 수정은_토큰이_없으면_401이고_일반회원은_403() throws Exception {
		mockMvc.perform(put("/api/v1/companies/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(minimalBody(SUB_CATEGORY)))
			.andExpect(status().isUnauthorized());

		update(general, minimalBody(SUB_CATEGORY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_MEMBER_REQUIRED"));
	}

	@Test
	void 수정할_때도_입력값과_카테고리를_검사한다() throws Exception {
		registerAndGetId(minimalBody(SUB_CATEGORY));

		update(company, minimalBody(SUB_CATEGORY).replace(",\"employeeCount\":12", ""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

		update(company, minimalBody(PARENT_CATEGORY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"));
	}

	@Test
	void 수정할_때_남의_파일을_쓰면_403이고_아무것도_바뀌지_않는다() throws Exception {
		long companyId = registerAndGetId(fullBody());
		Long othersImage = saveFile(companyWithoutPlan, FilePurpose.COMPANY_PORTFOLIO, "image/png");

		update(company, updatedBody().replace("\"portfolioFileIds\":[" + portfolioImage,
				"\"portfolioFileIds\":[" + othersImage))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FILE_ACCESS_DENIED"));

		assertThat(companyRepository.findById(companyId).orElseThrow().getName()).isEqualTo("팩허브 팩토리");
		assertThat(companyCertificationRepository.count()).isEqualTo(2);
		assertThat(companyImageRepository.count()).isEqualTo(3);
	}

	@Test
	void 목록은_로그인_없이_최신순으로_카드_정보와_페이지_정보를_반환한다() throws Exception {
		long first = saveCompany("가나 포장", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long second = registerAndGetId(fullBody());

		mockMvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.content[0].companyId").value(second))
			.andExpect(jsonPath("$.data.content[0].name").value("팩허브 팩토리"))
			.andExpect(jsonPath("$.data.content[0].industry").value("그라비아 연포장"))
			.andExpect(jsonPath("$.data.content[0].products").value("스탠딩파우치, 지퍼파우치"))
			.andExpect(jsonPath("$.data.content[0].address").value("경기 안산시 단원구 1"))
			.andExpect(jsonPath("$.data.content[0].logo.fileId").value(logo))
			.andExpect(jsonPath("$.data.content[0].logo.url").value(containsString("company-logo/")))
			.andExpect(jsonPath("$.data.content[1].companyId").value(first))
			.andExpect(jsonPath("$.data.content[1].logo").doesNotExist())
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(24))
			.andExpect(jsonPath("$.data.totalElements").value(2))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	void 목록에_숨김_상태이거나_삭제한_업체는_나오지_않는다() throws Exception {
		long visible = saveCompany("보이는 업체", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long hidden = saveCompany("숨긴 업체", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long deleted = saveCompany("삭제한 업체", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", hidden);
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `companyId` = ?", deleted);

		mockMvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].companyId").value(visible))
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void 등록한_업체가_없으면_빈_목록이다() throws Exception {
		mockMvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(0))
			.andExpect(jsonPath("$.data.totalPages").value(0));
	}

	@Test
	void 검색어는_업체명_업종_생산품목_중_하나에_포함되면_찾는다() throws Exception {
		long byName = saveCompany("한라 박스", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long byIndustry = saveCompany("가나 포장", "골판지 박스 제조", "상자", SUB_CATEGORY, "2010-01-01", 10);
		long byProducts = saveCompany("다라 산업", "지류", "택배 박스, 완충재", SUB_CATEGORY, "2010-01-01", 10);
		saveCompany("마바 필름", "필름", "랩", SUB_CATEGORY, "2010-01-01", 10);

		mockMvc.perform(get("/api/v1/companies").param("keyword", " 박스 "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.content[*].companyId")
				.value(org.hamcrest.Matchers.contains((int) byProducts, (int) byIndustry, (int) byName)));
	}

	@Test
	void 검색어의_퍼센트와_밑줄은_글자_그대로_찾는다() throws Exception {
		long percent = saveCompany("100% 재생지", "지류", "재생지", SUB_CATEGORY, "2010-01-01", 10);
		saveCompany("가나 포장", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);

		mockMvc.perform(get("/api/v1/companies").param("keyword", "%"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].companyId").value(percent));

		mockMvc.perform(get("/api/v1/companies").param("keyword", "_"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 카테고리는_소분류면_그_소분류만_대분류면_아래_소분류_전체에서_찾는다() throws Exception {
		long sub101 = saveCompany("삼방 업체", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long sub102 = saveCompany("사방 업체", "연포장", "파우치", OTHER_SUB_CATEGORY, "2010-01-01", 10);
		saveCompany("다른 대분류 업체", "지류", "상자", OTHER_PARENT_SUB_CATEGORY, "2010-01-01", 10);

		mockMvc.perform(get("/api/v1/companies").param("categoryId", String.valueOf(SUB_CATEGORY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].companyId").value(sub101));

		mockMvc.perform(get("/api/v1/companies").param("categoryId", String.valueOf(PARENT_CATEGORY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(2))
			.andExpect(jsonPath("$.data.content[*].companyId")
				.value(org.hamcrest.Matchers.contains((int) sub102, (int) sub101)));
	}

	@Test
	void 검색어와_카테고리를_같이_보내면_둘_다_맞는_업체만_찾는다() throws Exception {
		long match = saveCompany("한라 박스", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		saveCompany("가나 박스", "연포장", "파우치", OTHER_PARENT_SUB_CATEGORY, "2010-01-01", 10);
		saveCompany("다라 필름", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);

		mockMvc.perform(get("/api/v1/companies")
				.param("keyword", "박스")
				.param("categoryId", String.valueOf(PARENT_CATEGORY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].companyId").value(match));
	}

	@Test
	void 없는_카테고리로_찾으면_400() throws Exception {
		mockMvc.perform(get("/api/v1/companies").param("categoryId", "999999"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"));
	}

	@Test
	void 개업일순은_설립일이_오래된_업체부터_규모순은_직원이_많은_업체부터다() throws Exception {
		long oldSmall = saveCompany("오래된 작은 업체", "연포장", "파우치", SUB_CATEGORY, "1995-05-01", 5);
		long newBig = saveCompany("새 큰 업체", "연포장", "파우치", SUB_CATEGORY, "2022-01-01", 300);
		long middle = saveCompany("중간 업체", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 50);

		mockMvc.perform(get("/api/v1/companies").param("sort", "FOUNDED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].companyId")
				.value(org.hamcrest.Matchers.contains((int) oldSmall, (int) middle, (int) newBig)));

		mockMvc.perform(get("/api/v1/companies").param("sort", "SIZE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].companyId")
				.value(org.hamcrest.Matchers.contains((int) newBig, (int) middle, (int) oldSmall)));
	}

	@Test
	void 페이지를_나눠서_반환하고_범위를_넘은_페이지는_비어_있다() throws Exception {
		long first = saveCompany("업체 1", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long second = saveCompany("업체 2", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);
		long third = saveCompany("업체 3", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);

		mockMvc.perform(get("/api/v1/companies").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].companyId")
				.value(org.hamcrest.Matchers.contains((int) third, (int) second)))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(2));

		mockMvc.perform(get("/api/v1/companies").param("size", "2").param("page", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].companyId").value(first))
			.andExpect(jsonPath("$.data.page").value(2));

		mockMvc.perform(get("/api/v1/companies").param("size", "2").param("page", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(3));
	}

	@Test
	void 너무_큰_페이지_번호는_400() throws Exception {
		saveCompany("업체 1", "연포장", "파우치", SUB_CATEGORY, "2010-01-01", 10);

		// 100000페이지까지는 받는다. 업체가 없는 페이지라 빈 목록이다
		mockMvc.perform(get("/api/v1/companies").param("page", "100000").param("size", "50"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(1));

		for (String page : new String[] {"100001", "100000000", "2147483647"}) {
			mockMvc.perform(get("/api/v1/companies").param("page", page))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		}
	}

	@Test
	void 목록_조회_조건이_잘못되면_400() throws Exception {
		for (String[] param : new String[][] {{"page", "0"}, {"size", "0"}, {"size", "51"}, {"sort", "POPULAR"},
			{"categoryId", "abc"}, {"keyword", "가".repeat(51)}}) {
			mockMvc.perform(get("/api/v1/companies").param(param[0], param[1]))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		}
	}

	@Test
	void 분류_목록은_로그인_없이_대분류_37개와_소분류를_순서대로_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/categories").param("domain", "COMMON"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(37))
			.andExpect(jsonPath("$.data[0].categoryId").value(100))
			.andExpect(jsonPath("$.data[0].name").value("연포장·파우치"))
			.andExpect(jsonPath("$.data[0].children.length()").value(10))
			.andExpect(jsonPath("$.data[0].children[0].categoryId").value(101))
			.andExpect(jsonPath("$.data[0].children[0].name").value("삼방봉투"))
			.andExpect(jsonPath("$.data[0].children[9].name").value("자동포장용 롤필름"))
			.andExpect(jsonPath("$.data[36].categoryId").value(3700))
			.andExpect(jsonPath("$.data[36].name").value("디자인·개발·시험·설비서비스"))
			.andExpect(jsonPath("$.data[36].children.length()").value(17));
	}

	@Test
	void 아직_넣지_않은_사용처의_분류_목록은_비어_있다() throws Exception {
		mockMvc.perform(get("/api/v1/categories").param("domain", "JOB"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	void 분류_목록에_없는_사용처를_보내면_400() throws Exception {
		mockMvc.perform(get("/api/v1/categories").param("domain", "COMPANY"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 보유_인증_목록은_로그인_없이_35개를_코드와_표시명으로_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/certification-types"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(35))
			.andExpect(jsonPath("$.data[0].code").value("ISO_9001"))
			.andExpect(jsonPath("$.data[0].label").value("품질경영 인증(ISO 9001)"))
			.andExpect(jsonPath("$.data[0].description").value("품질·공정 관리체계"))
			.andExpect(jsonPath("$.data[34].code").value("KOLAS"));
	}

	private ResultActions register(long memberId, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/companies")
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private long registerAndGetId(String body) throws Exception {
		Number companyId = JsonPath.read(register(company, body)
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.data.companyId");
		return companyId.longValue();
	}

	private ResultActions update(long memberId, String body) throws Exception {
		return mockMvc.perform(put("/api/v1/companies/me")
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	/** 수정 요청. 등록(fullBody)에서 이름·카테고리·직원 수를 바꾸고, 로고·상세 주소·업체 사진을 빼고, 목록을 줄였다 */
	private String updatedBody() {
		return """
			{"name":"팩허브 패키징","industry":"그라비아 연포장","products":"스탠딩파우치","categoryId":%d,\
			"businessHours":"08:00 - 17:00","contactPhone":"02-1234-5678","address":"경기 안산시 단원구 1",\
			"representativeName":"홍길동","foundedDate":"2015-03-02","description":"연포장 전문 업체입니다.",\
			"employeeCount":30,"homepageUrl":"https://packhub.example.com","backgroundFileId":%d,\
			"certTypes":["ISO_9001","KS"],"equipments":[{"name":"드라이 라미네이터","imageFileId":%d}],\
			"portfolioFileIds":[%d],"officeFileIds":[]}\
			""".formatted(OTHER_SUB_CATEGORY, background, equipmentImage2, portfolioImage);
	}

	/** 새 기업회원을 만들고 그 회원의 업체를 API를 거치지 않고 바로 저장한다 (목록 조회용) */
	private long saveCompany(String name, String industry, String products, long categoryId, String foundedDate,
		int employeeCount) {
		int number = ++memberSequence;
		Long owner = saveMember("co-owner" + number + "@test.com", "010-9200-%04d".formatted(number), MemberType.COMPANY);
		Long companyId = companyRepository.save(Company.builder()
			.memberId(owner)
			.name(name)
			.industry(industry)
			.products(products)
			.businessHours("09:00 - 18:00")
			.contactPhone("02-1234-5678")
			.address("경기 안산시 단원구 1")
			.representativeName("홍길동")
			.foundedDate(LocalDate.parse(foundedDate))
			.employeeCount(employeeCount)
			.description("설명")
			.build()).getCompanyId();
		companyCategoryRepository.save(CompanyCategory.of(companyId, categoryId));
		return companyId;
	}

	/** 필수값만 채운 요청. 홈페이지·로고·인증·설비·사진은 보내지 않는다 */
	private String minimalBody(long categoryId) {
		return """
			{"name":"팩허브 팩토리","industry":"그라비아 연포장","products":"스탠딩파우치, 지퍼파우치","categoryId":%d,\
			"businessHours":"09:00 - 18:00","contactPhone":"02-1234-5678","address":"경기 안산시 단원구 1",\
			"representativeName":"홍길동","foundedDate":"2015-03-02","description":"연포장 전문 업체입니다.","employeeCount":12}\
			""".formatted(categoryId);
	}

	/** 선택 항목까지 모두 채운 요청. 인증은 일부러 HACCP을 두 번 넣는다 */
	private String fullBody() {
		String optional = """
			,"addressDetail":"2층","logoFileId":%d,"backgroundFileId":%d,"certTypes":["HACCP","ISO_9001","HACCP"],\
			"equipments":[{"name":"그라비아 인쇄기","imageFileId":%d},{"name":"드라이 라미네이터","imageFileId":%d}],\
			"portfolioFileIds":[%d,%d],"officeFileIds":[%d]}\
			""".formatted(logo, background, equipmentImage1, equipmentImage2, portfolioPdf, portfolioImage, officeImage);
		String minimal = minimalBody(SUB_CATEGORY);
		return minimal.substring(0, minimal.lastIndexOf('}')) + optional;
	}

	private List<CompanyImage> imagesOf(CompanyImageType imageType) {
		return companyImageRepository.findAll().stream()
			.filter(image -> image.getImageType() == imageType)
			.toList();
	}

	private <T> List<T> sorted(List<T> rows, java.util.function.ToIntFunction<T> sortOrder) {
		return rows.stream().sorted(Comparator.comparingInt(sortOrder)).toList();
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	/** 기업회원은 사업자 인증을 마친 상태로 만든다 (업체 등록·결제의 조건) */
	private Long saveMember(String email, String phone, MemberType memberType) {
		Long memberId = saveUnverifiedMember(email, phone, memberType);
		if (memberType == MemberType.COMPANY) {
			businessVerificationRepository.save(BusinessVerification.approveNow(memberId, "테스트 업체", "테스트",
				"123-45-67891", 1L, LocalDateTime.now()));
		}
		return memberId;
	}

	private Long saveUnverifiedMember(String email, String phone, MemberType memberType) {
		return memberRepository.save(Member.signUp(email, "{noop}password", "테스트", phone, memberType)).getMemberId();
	}

	/** S3에 올리지 않고 File 행만 만든다. 저장 키는 실제 규칙({용도 폴더}/{회원 ID}/{UUID}.{확장자})을 따른다 */
	private Long saveFile(Long uploaderId, FilePurpose purpose, String contentType) {
		String extension = contentType.substring(contentType.indexOf('/') + 1);
		String storageKey = purpose.getFolder() + "/" + uploaderId + "/" + UUID.randomUUID() + "." + extension;
		return fileRepository.save(File.register(uploaderId, "원본." + extension, storageKey, contentType, 1024))
			.getFileId();
	}
}
