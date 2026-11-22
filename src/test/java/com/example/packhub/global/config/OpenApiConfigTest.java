package com.example.packhub.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void API_문서는_토큰_없이_열리고_Bearer_인증_방식이_들어있다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
	}

	@Test
	void 로그인_회원_파라미터는_API_문서에_입력칸으로_나오지_않는다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/subscriptions/me'].get.parameters").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/subscriptions'].post.parameters").doesNotExist());
	}

	@Test
	void 인증_API_중_로그아웃만_Authorize_토큰을_쓴다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/auth/reissue'].post.security").isEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security[0].bearerAuth").exists());
	}

	@Test
	void Swagger_화면은_토큰_없이_열린다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}
}
