package com.example.packhub.domain.subscription.entity;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerConsultation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long partnerConsultationId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false, length = 100)
	private String companyName;

	@Column(nullable = false, length = 50)
	private String managerName;

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ConsultationStatus status;

	@Column(nullable = false)
	private LocalDateTime privacyAgreedAt;

	public static PartnerConsultation receive(Long memberId, String companyName, String managerName, String phone,
		String email, String content, LocalDateTime privacyAgreedAt) {
		PartnerConsultation consultation = new PartnerConsultation();
		consultation.memberId = memberId;
		consultation.companyName = companyName;
		consultation.managerName = managerName;
		consultation.phone = phone;
		consultation.email = email;
		consultation.content = content;
		consultation.status = ConsultationStatus.RECEIVED;
		consultation.privacyAgreedAt = privacyAgreedAt;
		return consultation;
	}
}
