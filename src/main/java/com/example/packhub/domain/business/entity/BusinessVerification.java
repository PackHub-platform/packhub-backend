package com.example.packhub.domain.business.entity;

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
 * 기업회원의 사업자 인증 신청 (가입 1-4). 거절되면 다시 신청할 수 있어서 회원 1명에 여러 행이 생길 수 있고,
 * 지금 상태는 가장 최근 신청으로 본다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessVerification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long businessVerificationId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false, length = 100)
	private String businessName;

	@Column(nullable = false, length = 50)
	private String representativeName;

	/** "000-00-00000" 형식 */
	@Column(nullable = false, length = 12)
	private String businessNumber;

	@Column(nullable = false)
	private Long certificateFileId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BusinessVerificationStatus status;

	@Column(length = 500)
	private String rejectReason;

	/** 검토한 관리자. 자동 승인이면 null */
	private Long reviewedBy;

	private LocalDateTime reviewedAt;

	/** 중간 범위: 형식 검사만 하고 바로 승인한다 (2026-11-22 명세) */
	public static BusinessVerification approveNow(Long memberId, String businessName, String representativeName,
		String businessNumber, Long certificateFileId, LocalDateTime now) {
		BusinessVerification verification = new BusinessVerification();
		verification.memberId = memberId;
		verification.businessName = businessName;
		verification.representativeName = representativeName;
		verification.businessNumber = businessNumber;
		verification.certificateFileId = certificateFileId;
		verification.status = BusinessVerificationStatus.APPROVED;
		verification.reviewedAt = now;
		return verification;
	}
}
