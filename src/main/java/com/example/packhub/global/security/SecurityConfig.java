package com.example.packhub.global.security;

import com.example.packhub.global.security.jwt.JwtAuthenticationFilter;
import com.example.packhub.global.security.jwt.JwtProperties;
import com.example.packhub.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	/** 로그인 없이 모든 메서드로 열어 두는 경로 */
	private static final String[] PUBLIC_PATHS = {
		"/api/v1/auth/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/v3/api-docs/**",
		"/error"
	};

	/** 로그인 없이 조회(GET)만 열어 두는 경로. 다른 도메인도 조회 공개가 필요하면 여기에 추가한다 */
	private static final String[] PUBLIC_GET_PATHS = {
		"/api/v1/terms/**",
		"/api/v1/requests/**"
	};

	private final JwtAuthenticationEntryPoint authenticationEntryPoint;
	private final JwtAccessDeniedHandler accessDeniedHandler;
	private final JwtProvider jwtProvider;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// JWT를 헤더로 보내는 API 서버라 쿠키 기반 공격(CSRF) 방어와 폼 로그인·세션이 필요 없다
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())
			// 아이디·비밀번호 폼 로그인 필터 자리 앞에서 토큰을 먼저 확인한다
			.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	/** 비밀번호 해시. 기본은 BCrypt이고, 저장값 앞에 알고리즘 이름이 붙어 나중에 바꾸기 쉽다 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
