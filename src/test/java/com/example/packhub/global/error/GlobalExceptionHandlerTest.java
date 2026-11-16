package com.example.packhub.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 성공_응답은_success와_data를_반환한다() throws Exception {
		mockMvc.perform(get("/test/ok"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").value("hello"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void 비즈니스_예외는_에러코드의_상태와_코드를_반환한다() throws Exception {
		mockMvc.perform(get("/test/business"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.error.fieldErrors").doesNotExist());
	}

	@Test
	void 검증_실패는_400과_필드_에러를_반환한다() throws Exception {
		mockMvc.perform(post("/test/valid")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));
	}

	@Test
	void 잘못된_JSON은_400을_반환한다() throws Exception {
		mockMvc.perform(post("/test/valid")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{invalid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 없는_경로는_404를_반환한다() throws Exception {
		mockMvc.perform(get("/test/unknown"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void 지원하지_않는_메서드는_405를_반환한다() throws Exception {
		mockMvc.perform(post("/test/ok"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void 처리되지_않은_예외는_500을_반환한다() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/ok")
		ApiResponse<String> ok() {
			return ApiResponse.ok("hello");
		}

		@GetMapping("/test/business")
		ApiResponse<Void> business() {
			throw new BusinessException(ErrorCode.NOT_FOUND);
		}

		@PostMapping("/test/valid")
		ApiResponse<Void> valid(@Valid @RequestBody TestRequest request) {
			return ApiResponse.ok();
		}

		@GetMapping("/test/unexpected")
		ApiResponse<Void> unexpected() {
			throw new IllegalStateException("boom");
		}
	}

	record TestRequest(@NotBlank String name) {
	}
}
