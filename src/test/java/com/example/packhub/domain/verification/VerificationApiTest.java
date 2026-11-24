package com.example.packhub.domain.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.member.repository.RefreshTokenRepository;
import com.example.packhub.domain.terms.repository.MemberTermsAgreementRepository;
import com.example.packhub.domain.verification.repository.VerificationCodeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 이메일·휴대폰 인증번호 (1-3)와 가입 때의 인증 검사.
 * 테스트는 local 프로필이라 발송 응답에 인증번호(code)가 들어온다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VerificationApiTest {

	private static final String EMAIL = "vf@test.com";
	private static final String PHONE = "01097100001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VerificationCodeRepository verificationCodeRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private MemberTermsAgreementRepository agreementRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		verificationCodeRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		agreementRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 이메일_인증번호를_보내고_맞게_입력하면_인증된다() throws Exception {
		String code = sendAndGetCode("email", "email", EMAIL);
		assertThat(code).matches("\\d{6}");

		confirm("email", "email", EMAIL, code)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.target").value(EMAIL))
			.andExpect(jsonPath("$.data.verified").value(true))
			.andExpect(jsonPath("$.data.verifiedAt").exists());
		// 원문은 저장하지 않는다
		assertThat(verificationCodeRepository.findAll().get(0).getCodeHash()).isNotEqualTo(code).hasSize(64);
	}

	@Test
	void 발송_응답에는_남은_시간과_재발송_시각이_있다() throws Exception {
		send("phone", "phone", PHONE)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.target").value(PHONE))
			.andExpect(jsonPath("$.data.expiresAt").exists())
			.andExpect(jsonPath("$.data.resendAvailableAt").exists())
			.andExpect(jsonPath("$.data.code").exists());
	}

	@Test
	void 휴대폰_인증번호가_틀리면_400이고_5번_틀리면_다시_보내야_한다() throws Exception {
		String code = sendAndGetCode("phone", "phone", PHONE);
		String wrong = code.equals("000000") ? "111111" : "000000";

		for (int i = 0; i < 5; i++) {
			confirm("phone", "phone", PHONE, wrong)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_MISMATCH"));
		}
		// 맞는 번호여도 이미 5번 틀렸으면 막는다
		confirm("phone", "phone", PHONE, code)
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.error.code").value("VERIFICATION_ATTEMPTS_EXCEEDED"));
	}

	@Test
	void 오분이_지나면_만료되고_발송_기록이_없으면_404다() throws Exception {
		String code = sendAndGetCode("email", "email", EMAIL);
		jdbcTemplate.update("UPDATE VerificationCode SET expiresAt = DATE_SUB(NOW(), INTERVAL 1 SECOND)");

		confirm("email", "email", EMAIL, code)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_EXPIRED"));
		confirm("email", "email", "nobody@test.com", "123456")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("VERIFICATION_NOT_FOUND"));
	}

	@Test
	void 육십초_안에_다시_보내면_429이고_지나면_새_번호가_가고_이전_번호는_안_된다() throws Exception {
		String first = sendAndGetCode("email", "email", EMAIL);

		send("email", "email", EMAIL)
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.error.code").value("VERIFICATION_RESEND_TOO_EARLY"));

		jdbcTemplate.update("UPDATE VerificationCode SET createdAt = DATE_SUB(NOW(), INTERVAL 61 SECOND)");
		String second = sendAndGetCode("email", "email", EMAIL);
		if (!first.equals(second)) {
			confirm("email", "email", EMAIL, first)
				.andExpect(jsonPath("$.error.code").value("VERIFICATION_CODE_MISMATCH"));
		}
		confirm("email", "email", EMAIL, second).andExpect(status().isOk());
	}

	@Test
	void 이미_가입된_이메일과_번호에는_보내지_않는다() throws Exception {
		memberRepository.save(Member.signUp(EMAIL, "{noop}password", "김팩허", PHONE, MemberType.GENERAL));

		send("email", "email", EMAIL)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
		send("phone", "phone", PHONE)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("MEMBER_PHONE_DUPLICATED"));
	}

	@Test
	void 형식이_틀리면_400이다() throws Exception {
		send("email", "email", "not-email").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		send("phone", "phone", "010-1234-5678").andExpect(status().isBadRequest());
		confirm("email", "email", EMAIL, "12ab").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("code"));
	}

	@Test
	void 이메일과_휴대폰을_둘_다_인증해야_가입된다() throws Exception {
		signUp().andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));

		confirm("email", "email", EMAIL, sendAndGetCode("email", "email", EMAIL)).andExpect(status().isOk());
		signUp().andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("PHONE_NOT_VERIFIED"));

		confirm("phone", "phone", PHONE, sendAndGetCode("phone", "phone", PHONE)).andExpect(status().isOk());
		signUp().andExpect(status().isCreated());
		assertThat(memberRepository.findByEmail(EMAIL)).isPresent();
	}

	@Test
	void 인증하고_30분이_지나면_다시_인증해야_가입된다() throws Exception {
		confirm("email", "email", EMAIL, sendAndGetCode("email", "email", EMAIL)).andExpect(status().isOk());
		confirm("phone", "phone", PHONE, sendAndGetCode("phone", "phone", PHONE)).andExpect(status().isOk());
		jdbcTemplate.update("UPDATE VerificationCode SET verifiedAt = DATE_SUB(NOW(), INTERVAL 31 MINUTE)");

		signUp().andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));
	}

	private String sendAndGetCode(String path, String field, String target) throws Exception {
		return JsonPath.read(send(path, field, target).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(), "$.data.code");
	}

	private ResultActions send(String path, String field, String target) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/" + path + "-verifications")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"%s\":\"%s\"}".formatted(field, target)));
	}

	private ResultActions confirm(String path, String field, String target, String code) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/" + path + "-verifications/confirm")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"%s\":\"%s\",\"code\":\"%s\"}".formatted(field, target, code)));
	}

	private ResultActions signUp() throws Exception {
		return mockMvc.perform(post("/api/v1/auth/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"memberType":"GENERAL","email":"%s","password":"Packhub!2026","passwordConfirm":"Packhub!2026","phone":"%s","agreements":[{"termsId":1,"agreed":true},{"termsId":2,"agreed":true},{"termsId":3,"agreed":true}]}
				""".formatted(EMAIL, PHONE)));
	}
}
