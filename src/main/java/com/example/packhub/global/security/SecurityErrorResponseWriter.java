package com.example.packhub.global.security;

import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.error.ErrorCode;
import com.example.packhub.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Security 필터 단계의 401·403을 공통 실패 형식으로 쓴다.
 * 필터는 DispatcherServlet 앞에서 동작해서 GlobalExceptionHandler가 잡지 못하기 때문에 따로 둔다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ApiResponse.fail(ErrorResponse.of(errorCode)));
	}
}
