package com.example.packhub.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TokenApiTest {

	private static final String PASSWORD = "Packhub!2026";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM RefreshToken");
		jdbcTemplate.update("DELETE FROM Member");
	}

	@Test
	void 로그인하면_Refresh_Token을_주고_DB에는_원문_대신_해시만_저장한다() throws Exception {
		signUp("a@example.com", "01011111111");
		String body = login("a@example.com")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.refreshToken").isString())
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
			.andReturn().getResponse().getContentAsString();
		String refreshToken = JsonPath.read(body, "$.data.refreshToken");

		List<String> hashes = jdbcTemplate.queryForList("SELECT tokenHash FROM RefreshToken", String.class);
		assertThat(hashes).hasSize(2) // 가입 1개 + 로그인 1개
			.doesNotContain(refreshToken)
			.allSatisfy(hash -> assertThat(hash).hasSize(64));
	}

	@Test
	void 재발급하면_새_토큰_한_쌍을_주고_새_Access_Token으로_막힌_경로를_호출할_수_있다() throws Exception {
		String refreshToken = signUp("a@example.com", "01011111111");

		String body = reissue(refreshToken)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
			.andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
			.andReturn().getResponse().getContentAsString();
		String newAccess = JsonPath.read(body, "$.data.accessToken");
		String newRefresh = JsonPath.read(body, "$.data.refreshToken");
		assertThat(newRefresh).isNotEqualTo(refreshToken);

		mockMvc.perform(get("/api/v1/subscriptions/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess))
			.andExpect(status().isOk());
		reissue(newRefresh).andExpect(status().isOk());
	}

	@Test
	void 한_번_쓴_토큰과_없는_토큰은_401_INVALID_REFRESH_TOKEN() throws Exception {
		String refreshToken = signUp("a@example.com", "01011111111");
		reissue(refreshToken).andExpect(status().isOk());

		reissue(refreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
		reissue("made-up-token")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void 기간이_지난_토큰은_401_REFRESH_TOKEN_EXPIRED() throws Exception {
		String refreshToken = signUp("a@example.com", "01011111111");
		jdbcTemplate.update("UPDATE RefreshToken SET expiresAt = DATE_SUB(NOW(), INTERVAL 1 SECOND)");

		reissue(refreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_EXPIRED"));
	}

	@Test
	void 같은_토큰으로_동시에_재발급하면_한_번만_성공한다() throws Exception {
		String refreshToken = signUp("a@example.com", "01011111111");
		int requests = 5;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(requests);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			Callable<Integer> call = () -> {
				start.await();
				return reissue(refreshToken).andReturn().getResponse().getStatus();
			};
			results.add(executor.submit(call));
		}
		start.countDown();

		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		executor.shutdown();

		assertThat(statuses).filteredOn(code -> code == 200).hasSize(1);
		assertThat(statuses).filteredOn(code -> code == 401).hasSize(requests - 1);
	}

	@Test
	void 정지된_회원은_재발급을_받을_수_없다() throws Exception {
		String refreshToken = signUp("a@example.com", "01011111111");
		jdbcTemplate.update("UPDATE Member SET status = 'SUSPENDED'");

		reissue(refreshToken)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("MEMBER_SUSPENDED"));
	}

	@Test
	void 로그아웃하면_그_토큰으로_재발급할_수_없다() throws Exception {
		String body = signUpBody("a@example.com", "01011111111");
		String access = JsonPath.read(body, "$.data.accessToken");
		String refreshToken = JsonPath.read(body, "$.data.refreshToken");

		logout(access, refreshToken)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
		reissue(refreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void 로그아웃은_로그인이_필요하다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"refreshToken":"anything"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 남의_Refresh_Token으로_로그아웃해도_그_토큰은_폐기되지_않는다() throws Exception {
		String victimRefresh = signUp("a@example.com", "01011111111");
		String attackerAccess = JsonPath.read(signUpBody("b@example.com", "01022222222"), "$.data.accessToken");

		logout(attackerAccess, victimRefresh).andExpect(status().isOk());
		reissue(victimRefresh).andExpect(status().isOk());
	}

	/** 가입하고 응답의 Refresh Token을 돌려준다 */
	private String signUp(String email, String phone) throws Exception {
		return JsonPath.read(signUpBody(email, phone), "$.data.refreshToken");
	}

	private String signUpBody(String email, String phone) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberType":"GENERAL","email":"%s","password":"%s","passwordConfirm":"%s","phone":"%s","name":"김팩허"}
					""".formatted(email, PASSWORD, PASSWORD, phone)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.refreshToken").isString())
			.andReturn().getResponse().getContentAsString();
	}

	private ResultActions login(String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"email":"%s","password":"%s"}
				""".formatted(email, PASSWORD)));
	}

	private ResultActions reissue(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/reissue")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"refreshToken":"%s"}
				""".formatted(refreshToken)));
	}

	private ResultActions logout(String accessToken, String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/logout")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"refreshToken":"%s"}
				""".formatted(refreshToken)));
	}
}
