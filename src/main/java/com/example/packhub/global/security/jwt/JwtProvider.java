package com.example.packhub.global.security.jwt;

import com.example.packhub.global.security.LoginMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Access Token을 만들고 검증한다.
 * 토큰에는 회원 ID(sub)와 권한(role), 만료 시각(exp)만 담는다. 이메일·이름 같은 개인정보는 넣지 않는다 (토큰 내용은 누구나 디코딩할 수 있다).
 */
@Component
public class JwtProvider {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey key;
	private final JwtProperties properties;

	public JwtProvider(JwtProperties properties) {
		this.properties = properties;
		// 키가 32바이트보다 짧으면 여기서 WeakKeyException으로 서버 시작이 실패한다
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(Long memberId, String role) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(ROLE_CLAIM, role)
			.issuedAt(now)
			.expiration(new Date(now.getTime() + properties.accessTokenValidity().toMillis()))
			.signWith(key)
			.compact();
	}

	/**
	 * 서명과 만료 시각을 검증하고 회원 정보를 꺼낸다.
	 *
	 * @throws ExpiredJwtException 만료된 토큰
	 * @throws JwtException        위조됐거나 형식이 잘못된 토큰
	 */
	public LoginMember parse(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
		return new LoginMember(Long.valueOf(claims.getSubject()), claims.get(ROLE_CLAIM, String.class));
	}

	public long getAccessTokenValiditySeconds() {
		return properties.accessTokenValidity().toSeconds();
	}
}
