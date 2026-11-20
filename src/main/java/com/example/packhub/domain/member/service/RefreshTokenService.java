package com.example.packhub.domain.member.service;

import com.example.packhub.domain.member.entity.RefreshToken;
import com.example.packhub.domain.member.repository.RefreshTokenRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import com.example.packhub.global.security.jwt.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 발급·회전·폐기.
 * 토큰은 JWT가 아닌 랜덤 문자열이다. 어차피 DB에서 찾아 확인하므로 서명이 필요 없고,
 * 32바이트 랜덤 값이라 지어내서 맞힐 수 없다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	/** 새 토큰을 저장하고 원문을 돌려준다. 원문은 이 응답에서만 나가고 서버에는 남지 않는다 */
	@Transactional
	public String issue(Long memberId) {
		String rawToken = generate();
		LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProperties.refreshTokenValidity());
		refreshTokenRepository.save(RefreshToken.issue(memberId, hash(rawToken), expiresAt));
		return rawToken;
	}

	/**
	 * 받은 토큰을 폐기하고 그 토큰 주인의 회원 ID를 돌려준다. 호출한 쪽이 새 토큰 한 쌍을 발급한다.
	 *
	 * @throws BusinessException INVALID_REFRESH_TOKEN(없음·이미 씀·로그아웃함), REFRESH_TOKEN_EXPIRED(14일 지남)
	 */
	@Transactional
	public Long consume(String rawToken) {
		LocalDateTime now = LocalDateTime.now();
		RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		if (token.isRevoked()) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (token.isExpired(now)) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		}
		// 같은 토큰으로 동시에 들어온 요청 중 먼저 폐기한 쪽만 통과한다
		if (refreshTokenRepository.revokeIfActive(token.getRefreshTokenId(), now) == 0) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		return token.getMemberId();
	}

	/**
	 * 로그아웃. 본인 토큰이면 폐기한다.
	 * 없는 토큰·이미 폐기된 토큰·남의 토큰이면 아무것도 하지 않는다 (프론트는 결과와 상관없이 토큰을 지운다).
	 */
	@Transactional
	public void revoke(Long memberId, String rawToken) {
		refreshTokenRepository.findByTokenHash(hash(rawToken))
			.filter(token -> token.getMemberId().equals(memberId))
			.ifPresent(token -> refreshTokenRepository.revokeIfActive(token.getRefreshTokenId(), LocalDateTime.now()));
	}

	public long getValiditySeconds() {
		return jwtProperties.refreshTokenValidity().toSeconds();
	}

	private static String generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256을 지원하지 않는 환경입니다.", e);
		}
	}
}
