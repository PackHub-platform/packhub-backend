package com.example.packhub.global.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 jwt.* 설정.
 *
 * @param secret               서명용 비밀 키. HS256이라 32바이트(영문 32자) 이상이어야 한다. 서버에서는 환경 변수 JWT_SECRET으로 받는다
 * @param accessTokenValidity  Access Token 유효 시간 (예: 30m)
 * @param refreshTokenValidity Refresh Token 유효 시간 (예: 14d). Refresh Token은 JWT가 아니지만 토큰 설정이라 함께 둔다
 */
@ConfigurationProperties("jwt")
public record JwtProperties(String secret, Duration accessTokenValidity, Duration refreshTokenValidity) {
}
