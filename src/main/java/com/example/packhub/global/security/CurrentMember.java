package com.example.packhub.global.security;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 로그인한 회원의 ID를 컨트롤러 파라미터로 받는다.
 *
 * <pre>
 * public ApiResponse&lt;...&gt; getMine(@CurrentMember Long memberId) { ... }
 * </pre>
 *
 * JwtAuthenticationFilter가 SecurityContext에 넣어 둔 LoginMember에서 memberId만 꺼낸다.
 * 로그인이 필요한 경로(SecurityConfig에서 막힌 경로)에서만 쓴다. 토큰이 없으면 컨트롤러까지 오기 전에 401로 끝난다.
 * Swagger 화면에는 입력칸으로 나오지 않는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "memberId")
@Parameter(hidden = true)
public @interface CurrentMember {
}
