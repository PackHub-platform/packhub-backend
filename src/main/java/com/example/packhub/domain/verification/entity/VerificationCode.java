package com.example.packhub.domain.verification.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입할 때 보내는 이메일·휴대폰 인증번호 1건. 발송할 때마다 새 행이 생기고, 항상 가장 최근 행으로 판단한다.
 * 인증번호 원문은 저장하지 않고 SHA-256 해시만 저장한다 (Refresh Token과 같은 방식).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCode extends BaseTimeEntity {

	/** 이만큼 틀리면 다시 발송해야 한다 */
	public static final int MAX_ATTEMPTS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long verificationCodeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VerificationChannel channel;

	/** 이메일 또는 휴대폰 번호 */
	@Column(nullable = false, length = 255)
	private String target;

	@Column(nullable = false, length = 255)
	private String codeHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Column(nullable = false)
	private int attemptCount;

	private LocalDateTime verifiedAt;

	public static VerificationCode issue(VerificationChannel channel, String target, String codeHash,
		LocalDateTime expiresAt) {
		VerificationCode code = new VerificationCode();
		code.channel = channel;
		code.target = target;
		code.codeHash = codeHash;
		code.expiresAt = expiresAt;
		code.attemptCount = 0;
		return code;
	}

	public boolean isVerified() {
		return verifiedAt != null;
	}

	public boolean isExpired(LocalDateTime now) {
		return !now.isBefore(expiresAt);
	}

	public boolean attemptsExceeded() {
		return attemptCount >= MAX_ATTEMPTS;
	}

	public boolean matches(String codeHash) {
		return this.codeHash.equals(codeHash);
	}

	public void recordFailure() {
		this.attemptCount++;
	}

	public void markVerified(LocalDateTime now) {
		this.verifiedAt = now;
	}

	/** 인증을 마친 뒤 validMinutes분이 지나지 않았는지 (가입은 그 안에 마쳐야 한다) */
	public boolean isVerifiedWithin(LocalDateTime now, long validMinutes) {
		return verifiedAt != null && now.isBefore(verifiedAt.plusMinutes(validMinutes));
	}
}
