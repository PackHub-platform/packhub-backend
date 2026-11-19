package com.example.packhub.global.security;

import com.example.packhub.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증이 필요한 경로에 인증 없이 들어온 요청에 401을 응답한다.
 * 토큰이 만료됐으면 필터가 남긴 표시를 보고 TOKEN_EXPIRED로, 그 외에는 UNAUTHORIZED로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/** JwtAuthenticationFilter가 토큰 문제를 요청에 남길 때 쓰는 속성 이름 */
	public static final String ERROR_CODE_ATTRIBUTE = "jwtErrorCode";

	private final SecurityErrorResponseWriter responseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		Object errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE);
		responseWriter.write(response,
			errorCode instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED);
	}
}
