package com.example.packhub.domain.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.business.repository.BusinessVerificationRepository;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.member.repository.RefreshTokenRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 사업자 인증 신청·조회 (1-4). 중간 범위에서는 신청하면 바로 승인한다 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BusinessVerificationApiTest {

	/** 검증 숫자가 맞는 사업자번호 */
	private static final String VALID_NUMBER = "123-45-67891";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BusinessVerificationRepository businessVerificationRepository;

	@Autowired
	private FileRepository fileRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtProvider jwtProvider;

	private Long company;
	private Long general;
	private Long certificate;

	@BeforeEach
	void setUp() {
		company = saveMember("bv-company@test.com", "01095100001", MemberType.COMPANY);
		general = saveMember("bv-general@test.com", "01095100002", MemberType.GENERAL);
		certificate = saveFile(company, FilePurpose.BUSINESS_CERTIFICATE);
	}

	@AfterEach
	void cleanUp() {
		businessVerificationRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		fileRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 신청하면_바로_승인되고_내_상태로_조회된다() throws Exception {
		apply(company, body(VALID_NUMBER, certificate))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.businessVerificationId").exists())
			.andExpect(jsonPath("$.data.status").value("APPROVED"))
			.andExpect(jsonPath("$.data.reviewedAt").exists());

		mockMvc.perform(get("/api/v1/business-verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.businessName").value("팩허브 패키징"))
			.andExpect(jsonPath("$.data.representativeName").value("김팩허"))
			.andExpect(jsonPath("$.data.businessNumber").value(VALID_NUMBER))
			.andExpect(jsonPath("$.data.status").value("APPROVED"));
	}

	@Test
	void 이미_승인됐으면_다시_신청할_수_없다() throws Exception {
		apply(company, body(VALID_NUMBER, certificate)).andExpect(status().isCreated());

		apply(company, body(VALID_NUMBER, certificate))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("BUSINESS_VERIFICATION_ALREADY_APPROVED"));
		assertThat(businessVerificationRepository.count()).isEqualTo(1);
	}

	@Test
	void 사업자번호_형식이나_검증_숫자가_틀리면_400이다() throws Exception {
		for (String number : new String[] {"123-45-67890", "1234567891", "123-45-6789"}) {
			apply(company, body(number, certificate))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_BUSINESS_NUMBER"));
		}
		assertThat(businessVerificationRepository.count()).isZero();
	}

	@Test
	void 빈_칸이_있으면_400이다() throws Exception {
		apply(company, """
			{"businessName":"","representativeName":"김팩허","businessNumber":"%s"}
			""".formatted(VALID_NUMBER))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 사업자등록증_용도가_아니거나_남의_파일이면_막는다() throws Exception {
		Long companyImage = saveFile(company, FilePurpose.COMPANY_IMAGE);
		Long othersCertificate = saveFile(general, FilePurpose.BUSINESS_CERTIFICATE);

		apply(company, body(VALID_NUMBER, companyImage))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("FILE_PURPOSE_MISMATCH"));
		apply(company, body(VALID_NUMBER, othersCertificate))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FILE_ACCESS_DENIED"));
		apply(company, body(VALID_NUMBER, 999999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
	}

	@Test
	void 일반회원은_신청도_조회도_403이고_로그인하지_않으면_401이다() throws Exception {
		apply(general, body(VALID_NUMBER, certificate))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("COMPANY_MEMBER_REQUIRED"));
		mockMvc.perform(get("/api/v1/business-verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/business-verifications/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 신청한_적_없으면_조회는_404다() throws Exception {
		mockMvc.perform(get("/api/v1/business-verifications/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("BUSINESS_VERIFICATION_NOT_FOUND"));
	}

	@Test
	void 로그인_응답에_사업자_인증_상태가_나온다() throws Exception {
		login("bv-company@test.com")
			.andExpect(jsonPath("$.data.member.businessVerificationStatus").value("NONE"));

		apply(company, body(VALID_NUMBER, certificate)).andExpect(status().isCreated());

		login("bv-company@test.com")
			.andExpect(jsonPath("$.data.member.businessVerificationStatus").value("APPROVED"));
	}

	private ResultActions apply(long memberId, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/business-verifications")
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions login(String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"%s\",\"password\":\"Packhub1!\"}".formatted(email)));
	}

	private String body(String businessNumber, Long fileId) {
		return """
			{"businessName":"팩허브 패키징","representativeName":"김팩허","businessNumber":"%s","certificateFileId":%d}
			""".formatted(businessNumber, fileId);
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone, MemberType memberType) {
		return memberRepository.save(Member.signUp(email, passwordEncoder.encode("Packhub1!"), "테스트", phone,
			memberType)).getMemberId();
	}

	private Long saveFile(Long uploaderId, FilePurpose purpose) {
		String storageKey = purpose.getFolder() + "/" + uploaderId + "/" + UUID.randomUUID() + ".png";
		return fileRepository.save(File.register(uploaderId, "원본.png", storageKey, "image/png", 1024)).getFileId();
	}
}
