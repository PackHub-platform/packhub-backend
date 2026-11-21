package com.example.packhub.domain.member.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급한 Refresh Token 한 개 (로그인 1회 = 1행).
 * 원문은 저장하지 않고 해시만 저장한다. DB가 유출돼도 해시로는 재발급을 받을 수 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long refreshTokenId;

	@Column(nullable = false)
	private Long memberId;

	/** 원문 토큰의 SHA-256 해시 (hex 64자) */
	@Column(nullable = false, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	/** 재발급(회전)·로그아웃으로 폐기된 시각. 비어 있으면 아직 쓸 수 있다 */
	private LocalDateTime revokedAt;

	public static RefreshToken issue(Long memberId, String tokenHash, LocalDateTime expiresAt) {
		RefreshToken token = new RefreshToken();
		token.memberId = memberId;
		token.tokenHash = tokenHash;
		token.expiresAt = expiresAt;
		return token;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}
}
