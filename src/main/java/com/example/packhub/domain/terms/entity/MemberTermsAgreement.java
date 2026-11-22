package com.example.packhub.domain.terms.entity;

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

/** 회원이 약관(버전)에 동의했는지 거부했는지 기록한다. 선택 약관을 거부한 것도 남긴다 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsAgreement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long memberTermsAgreementId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Long termsId;

	@Column(nullable = false)
	private boolean agreed;

	/** 동의·거부한 시각 */
	@Column(nullable = false)
	private LocalDateTime agreedAt;

	/** 동의를 철회한 시각 (마케팅 수신 해제 등). 철회 기능은 아직 없다 */
	private LocalDateTime withdrawnAt;

	public static MemberTermsAgreement of(Long memberId, Long termsId, boolean agreed, LocalDateTime agreedAt) {
		MemberTermsAgreement agreement = new MemberTermsAgreement();
		agreement.memberId = memberId;
		agreement.termsId = termsId;
		agreement.agreed = agreed;
		agreement.agreedAt = agreedAt;
		return agreement;
	}
}
