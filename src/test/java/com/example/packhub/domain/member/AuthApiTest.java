package com.example.packhub.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.verification.VerificationTestSupport;
import com.example.packhub.domain.verification.repository.VerificationCodeRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiTest {

	private static final String EMAIL = "packhub@example.com";
	private static final String PHONE = "01012345678";
	private static final String PASSWORD = "Packhub!2026";

	@Autowired
	private VerificationCodeRepository verificationCodeRepository;

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		verificationCodeRepository.deleteAllInBatch();
		jdbcTemplate.update("DELETE FROM RefreshToken");
		jdbcTemplate.update("DELETE FROM MemberTermsAgreement");
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 일반회원_가입은_201과_토큰을_주고_가입_완료로_안내한다() throws Exception {
		signUp("GENERAL", EMAIL, PHONE, PASSWORD, PASSWORD)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.memberType").value("GENERAL"))
			.andExpect(jsonPath("$.data.nextStep").value("DONE"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
			.andExpect(jsonPath("$.data.accessToken").isString());

		Member saved = memberRepository.findByEmail(EMAIL).orElseThrow();
		assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();
	}

	@Test
	void 기업회원_가입은_사업자_인증으로_안내한다() throws Exception {
		signUp("COMPANY", EMAIL, PHONE, PASSWORD, PASSWORD)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.nextStep").value("BUSINESS_VERIFICATION"));
	}

	@Test
	void 비밀번호_확인이_다르면_400() throws Exception {
		signUp("GENERAL", EMAIL, PHONE, PASSWORD, "Different!2026")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PASSWORD_CONFIRM_MISMATCH"));
	}

	@Test
	void 비밀번호_규칙과_휴대폰_형식이_틀리면_칸별_사유를_준다() throws Exception {
		signUp("GENERAL", EMAIL, "010-1234-5678", "onlyletters", "onlyletters")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors.length()").value(2));
	}

	@Test
	void 이미_가입된_이메일이나_휴대폰이면_409() throws Exception {
		signUp("GENERAL", EMAIL, PHONE, PASSWORD, PASSWORD).andExpect(status().isCreated());

		signUp("GENERAL", EMAIL, "01099998888", PASSWORD, PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
		signUp("GENERAL", "other@example.com", PHONE, PASSWORD, PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_PHONE_DUPLICATED"));
	}

	@Test
	void 중복_검사를_통과한_동시_가입도_DB_유니크_제약에서_409로_막힌다() throws Exception {
		signUp("GENERAL", EMAIL, PHONE, PASSWORD, PASSWORD).andExpect(status().isCreated());
		// 다른 요청이 검사와 저장 사이에 끼어든 상황: 중복 검사는 "없음"으로 통과시킨다
		given(memberRepository.existsByEmail(anyString())).willReturn(false);
		given(memberRepository.existsByPhone(anyString())).willReturn(false);

		signUp("GENERAL", EMAIL, "01099998888", PASSWORD, PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
		signUp("GENERAL", "other@example.com", PHONE, PASSWORD, PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_PHONE_DUPLICATED"));
		assertThat(memberRepository.count()).isEqualTo(1);
	}

	@Test
	void 로그인하면_회원_정보와_토큰을_주고_마지막_로그인_시각을_남긴다() throws Exception {
		Long memberId = signUpAndGetId("COMPANY");

		String token = JsonPath.read(login(EMAIL, PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.member.memberId").value(memberId))
			.andExpect(jsonPath("$.data.member.memberType").value("COMPANY"))
			.andExpect(jsonPath("$.data.member.role").value("USER"))
			.andExpect(jsonPath("$.data.member.businessVerificationStatus").value("NONE"))
			.andReturn().getResponse().getContentAsString(), "$.data.accessToken");

		assertThat(jwtProvider.parse(token).memberId()).isEqualTo(memberId);
		assertThat(memberRepository.findById(memberId).orElseThrow().getLastLoginAt()).isNotNull();
	}

	@Test
	void 없는_이메일과_틀린_비밀번호는_같은_401_LOGIN_FAILED() throws Exception {
		signUpAndGetId("GENERAL");

		login("nobody@example.com", PASSWORD)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
		login(EMAIL, "Wrong!2026")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
	}

	@Test
	void 정지된_계정은_비밀번호가_맞을_때만_정지_사실을_알려_준다() throws Exception {
		Long memberId = signUpAndGetId("GENERAL");
		jdbcTemplate.update("UPDATE Member SET status = 'SUSPENDED' WHERE memberId = ?", memberId);

		login(EMAIL, "Wrong!2026")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
		login(EMAIL, PASSWORD)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("MEMBER_SUSPENDED"));
	}

	private Long signUpAndGetId(String memberType) throws Exception {
		Number id = JsonPath.read(signUp(memberType, EMAIL, PHONE, PASSWORD, PASSWORD)
			.andReturn().getResponse().getContentAsString(), "$.data.memberId");
		return id.longValue();
	}

	private ResultActions signUp(String memberType, String email, String phone, String password,
		String passwordConfirm) throws Exception {
		VerificationTestSupport.markVerified(verificationCodeRepository, email, phone);
		return mockMvc.perform(post("/api/v1/auth/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"memberType":"%s","email":"%s","password":"%s","passwordConfirm":"%s","phone":"%s","name":"김팩허","agreements":[{"termsId":1,"agreed":true},{"termsId":2,"agreed":true},{"termsId":3,"agreed":true},{"termsId":4,"agreed":false},{"termsId":5,"agreed":false}]}
				""".formatted(memberType, email, password, passwordConfirm, phone)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"email":"%s","password":"%s"}
				""".formatted(email, password)));
	}
}
