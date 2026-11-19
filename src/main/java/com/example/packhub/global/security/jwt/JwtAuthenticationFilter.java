package com.example.packhub.global.security.jwt;

import com.example.packhub.global.error.ErrorCode;
import com.example.packhub.global.security.JwtAuthenticationEntryPoint;
import com.example.packhub.global.security.LoginMember;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 Authorization: Bearer 토큰을 읽어 로그인 표시를 남긴다.
 * 토큰이 없거나 잘못돼도 여기서 응답하지 않고 다음으로 넘긴다.
 * 열린 경로면 그대로 통과하고, 막힌 경로면 JwtAuthenticationEntryPoint가 401을 응답한다.
 *
 * @Component를 붙이지 않는다. 붙이면 Spring Boot가 Security 밖의 일반 필터로도 한 번 더 등록한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null) {
			try {
				LoginMember loginMember = jwtProvider.parse(token);
				var authentication = new UsernamePasswordAuthenticationToken(
					loginMember, null, List.of(new SimpleGrantedAuthority("ROLE_" + loginMember.role())));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (ExpiredJwtException e) {
				request.setAttribute(JwtAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
			} catch (JwtException | IllegalArgumentException e) {
				request.setAttribute(JwtAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, ErrorCode.UNAUTHORIZED);
			}
		}
		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}
		return null;
	}
}
