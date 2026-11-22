package com.example.packhub.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.packhub.global.security.LoginMember;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

	private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

	@Test
	void 만든_토큰을_검증하면_회원ID와_권한을_꺼낸다() {
		JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

		LoginMember loginMember = provider.parse(provider.createAccessToken(12L, "USER"));

		assertThat(loginMember.memberId()).isEqualTo(12L);
		assertThat(loginMember.role()).isEqualTo("USER");
	}

	@Test
	void 만료된_토큰은_ExpiredJwtException을_던진다() {
		JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(14)));
		String expired = provider.createAccessToken(12L, "USER");

		assertThatThrownBy(() -> provider.parse(expired)).isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void 다른_키로_서명한_토큰은_JwtException을_던진다() {
		JwtProvider other = new JwtProvider(
			new JwtProperties("another-secret-key-that-is-also-32-bytes!!", Duration.ofMinutes(30), Duration.ofDays(14)));
		JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));
		String forged = other.createAccessToken(12L, "ADMIN");

		assertThatThrownBy(() -> provider.parse(forged)).isInstanceOf(JwtException.class);
	}

	@Test
	void 비밀_키가_32바이트보다_짧으면_시작할_때_실패한다() {
		assertThatThrownBy(() -> new JwtProvider(new JwtProperties("too-short", Duration.ofMinutes(30), Duration.ofDays(14))))
			.isInstanceOf(WeakKeyException.class);
	}
}
