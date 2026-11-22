package com.example.packhub.domain.terms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.terms.entity.MemberTermsAgreement;
import com.example.packhub.domain.terms.repository.MemberTermsAgreementRepository;
import java.util.Map;
import java.util.stream.Collectors;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TermsApiTest {

	private static final String EMAIL = "terms@example.com";

	/** 시드(V20261122_0100) 기준 가입 약관: 1~3 필수, 4~5 선택 */
	private static final String ALL_REQUIRED_AGREED = """
		[{"termsId":1,"agreed":true},{"termsId":2,"agreed":true},{"termsId":3,"agreed":true},
		 {"termsId":4,"agreed":false},{"termsId":5,"agreed":true}]""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberTermsAgreementRepository agreementRepository;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM RefreshToken");
		agreementRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		jdbcTemplate.update("DELETE FROM Terms WHERE termsId > 9"); // 시드는 남기고 테스트에서 추가한 버전만 지운다
	}

	@Test
	void 약관_목록은_토큰_없이_가입_약관을_화면_순서대로_준다() throws Exception {
		mockMvc.perform(get("/api/v1/terms"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(5))
			.andExpect(jsonPath("$.data[0].termsType").value("SERVICE"))
			.andExpect(jsonPath("$.data[0].isRequired").value(true))
			.andExpect(jsonPath("$.data[4].termsType").value("MARKETING"))
			.andExpect(jsonPath("$.data[4].isRequired").value(false))
			.andExpect(jsonPath("$.data[0].content").doesNotExist());
	}

	@Test
	void 용도를_주면_그_용도의_약관만_준다() throws Exception {
		mockMvc.perform(get("/api/v1/terms").param("usage", "PAYMENT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].termsType").value("PAYMENT_REFUND"))
			.andExpect(jsonPath("$.data[1].termsType").value("RECURRING_PAYMENT"));
	}

	@Test
	void 없는_용도는_400() throws Exception {
		mockMvc.perform(get("/api/v1/terms").param("usage", "UNKNOWN"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 새_버전이_시행되면_목록과_본문은_새_버전만_주고_시행_전_버전은_숨긴다() throws Exception {
		insertTerms(10, "SERVICE", "2.0", "2026-11-10 00:00:00");
		insertTerms(11, "SERVICE", "3.0", "2099-01-01 00:00:00"); // 아직 시행 전

		mockMvc.perform(get("/api/v1/terms"))
			.andExpect(jsonPath("$.data.length()").value(5))
			.andExpect(jsonPath("$.data[0].termsId").value(10))
			.andExpect(jsonPath("$.data[0].version").value("2.0"));

		mockMvc.perform(get("/api/v1/terms/SERVICE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.version").value("2.0"));
	}

	@Test
	void 시행일이_같은_버전이_있으면_나중에_넣은_버전을_목록과_가입_검사_모두_쓴다() throws Exception {
		insertTerms(10, "SERVICE", "2.0", "2026-11-10 00:00:00");
		insertTerms(11, "SERVICE", "2.1", "2026-11-10 00:00:00");

		mockMvc.perform(get("/api/v1/terms"))
			.andExpect(jsonPath("$.data[0].termsId").value(11));
		signUp(ALL_REQUIRED_AGREED.replace("{\"termsId\":1,", "{\"termsId\":11,"))
			.andExpect(status().isCreated());
	}

	@Test
	void 본문은_최신_버전_또는_지정한_버전을_준다() throws Exception {
		mockMvc.perform(get("/api/v1/terms/PRIVACY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.termsId").value(2))
			.andExpect(jsonPath("$.data.content").isString());

		mockMvc.perform(get("/api/v1/terms/PRIVACY").param("version", "1.0"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.version").value("1.0"));
	}

	@Test
	void 없는_버전은_404_없는_약관_종류는_400() throws Exception {
		mockMvc.perform(get("/api/v1/terms/SERVICE").param("version", "9.9"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("TERMS_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/terms/UNKNOWN"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 가입하면_선택_약관_거부까지_동의_내역을_남긴다() throws Exception {
		signUp(ALL_REQUIRED_AGREED).andExpect(status().isCreated());

		Long memberId = memberRepository.findByEmail(EMAIL).orElseThrow().getMemberId();
		Map<Long, Boolean> agreedByTermsId = agreementRepository.findByMemberId(memberId).stream()
			.collect(Collectors.toMap(MemberTermsAgreement::getTermsId, MemberTermsAgreement::isAgreed));
		assertThat(agreedByTermsId).containsExactlyInAnyOrderEntriesOf(
			Map.of(1L, true, 2L, true, 3L, true, 4L, false, 5L, true));
	}

	@Test
	void 필수_약관을_거부하면_400이고_회원도_만들지_않는다() throws Exception {
		signUp("""
			[{"termsId":1,"agreed":true},{"termsId":2,"agreed":false},{"termsId":3,"agreed":true},
			 {"termsId":4,"agreed":true},{"termsId":5,"agreed":true}]""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("REQUIRED_TERMS_NOT_AGREED"));

		assertThat(memberRepository.findByEmail(EMAIL)).isEmpty();
	}

	@Test
	void 필수_약관이_빠져도_400() throws Exception {
		signUp("""
			[{"termsId":1,"agreed":true},{"termsId":2,"agreed":true}]""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("REQUIRED_TERMS_NOT_AGREED"));
	}

	@Test
	void 옛_버전_없는_ID_중복_ID가_섞이면_400() throws Exception {
		insertTerms(10, "SERVICE", "2.0", "2026-11-10 00:00:00"); // 이제 1번(1.0)은 옛 버전

		signUp(ALL_REQUIRED_AGREED)
			.andExpect(jsonPath("$.error.code").value("REQUIRED_TERMS_NOT_AGREED"));
		signUp(ALL_REQUIRED_AGREED.replace("{\"termsId\":1,", "{\"termsId\":10,").replace("]", ",{\"termsId\":999,\"agreed\":true}]"))
			.andExpect(jsonPath("$.error.code").value("REQUIRED_TERMS_NOT_AGREED"));
		signUp(ALL_REQUIRED_AGREED.replace("{\"termsId\":1,", "{\"termsId\":10,").replace("]", ",{\"termsId\":10,\"agreed\":false}]"))
			.andExpect(jsonPath("$.error.code").value("REQUIRED_TERMS_NOT_AGREED"));

		assertThat(memberRepository.findByEmail(EMAIL)).isEmpty();
	}

	@Test
	void 약관_동의를_안_보내면_입력_오류() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberType":"GENERAL","email":"%s","password":"Packhub!2026","passwordConfirm":"Packhub!2026","phone":"01055556666"}
					""".formatted(EMAIL)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("agreements"));
	}

	private ResultActions signUp(String agreements) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"memberType":"GENERAL","email":"%s","password":"Packhub!2026","passwordConfirm":"Packhub!2026","phone":"01055556666","agreements":%s}
				""".formatted(EMAIL, agreements)));
	}

	private void insertTerms(long termsId, String termsType, String version, String effectiveAt) {
		jdbcTemplate.update("""
			INSERT INTO Terms (termsId, termsType, version, title, content, isRequired, effectiveAt)
			VALUES (?, ?, ?, '테스트 약관', '본문', TRUE, ?)""", termsId, termsType, version, effectiveAt);
	}
}
