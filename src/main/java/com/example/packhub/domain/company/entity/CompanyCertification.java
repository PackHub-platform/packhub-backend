package com.example.packhub.domain.company.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업체가 고른 보유 인증. 인증 하나에 한 행이고, 같은 업체에 같은 인증은 한 번만 들어간다 (UNIQUE).
 * 인증서 이미지를 받을지는 미정이라 imageFileId는 비워 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyCertification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long companyCertificationId;

	@Column(nullable = false)
	private Long companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CertificationType certType;

	private Long imageFileId;

	@Column(nullable = false)
	private int sortOrder;

	public static CompanyCertification of(Long companyId, CertificationType certType, int sortOrder) {
		CompanyCertification certification = new CompanyCertification();
		certification.companyId = companyId;
		certification.certType = certType;
		certification.sortOrder = sortOrder;
		return certification;
	}
}
