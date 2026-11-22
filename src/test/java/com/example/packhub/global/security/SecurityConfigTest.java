package com.example.packhub.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.jwt.JwtProperties;
import com.example.packhub.global.security.jwt.JwtProvider;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class, properties = {
	"jwt.secret=test-jwt-secret-key-at-least-32-bytes-long",
	"jwt.access-token-validity=30m"
})
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class, SecurityErrorResponseWriter.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

	private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 열린_경로는_토큰_없이_통과한다() throws Exception {
		mockMvc.perform(get("/api/v1/requests/1"))
			.andExpect(status().isOk());
	}

	@Test
	void 막힌_경로에_토큰이_없으면_401_UNAUTHORIZED() throws Exception {
		mockMvc.perform(get("/api/v1/private"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 유효한_토큰이면_로그인한_회원으로_통과한다() throws Exception {
		String token = jwtProvider.createAccessToken(12L, "USER");

		mockMvc.perform(get("/api/v1/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(12));
	}

	@Test
	void 만료된_토큰이면_401_TOKEN_EXPIRED() throws Exception {
		String expired = new JwtProvider(new JwtProperties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(14)))
			.createAccessToken(12L, "USER");

		mockMvc.perform(get("/api/v1/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
	}

	@Test
	void 위조된_토큰이면_401_UNAUTHORIZED() throws Exception {
		String forged = new JwtProvider(
			new JwtProperties("another-secret-key-that-is-also-32-bytes!!", Duration.ofMinutes(30), Duration.ofDays(14)))
			.createAccessToken(12L, "ADMIN");

		mockMvc.perform(get("/api/v1/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 관리자_경로에_일반_회원이_오면_403_FORBIDDEN() throws Exception {
		String token = jwtProvider.createAccessToken(12L, "USER");

		mockMvc.perform(get("/api/v1/admin/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void 관리자_경로에_관리자는_통과한다() throws Exception {
		String token = jwtProvider.createAccessToken(1L, "ADMIN");

		mockMvc.perform(get("/api/v1/admin/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
	}

	@RestController
	static class TestController {

		@GetMapping("/api/v1/requests/1")
		ApiResponse<String> publicGet() {
			return ApiResponse.ok("public");
		}

		@GetMapping("/api/v1/private")
		ApiResponse<Long> privateGet(@AuthenticationPrincipal LoginMember loginMember) {
			return ApiResponse.ok(loginMember.memberId());
		}

		@GetMapping("/api/v1/admin/test")
		ApiResponse<String> adminGet() {
			return ApiResponse.ok("admin");
		}
	}
}
