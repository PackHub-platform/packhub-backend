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

/** 포트폴리오·업체 사진 (종류별 최대 10개). 파일만 저장한다. 포트폴리오는 PDF일 수도 있다 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long companyImageId;

	@Column(nullable = false)
	private Long companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CompanyImageType imageType;

	@Column(nullable = false)
	private Long fileId;

	@Column(nullable = false)
	private int sortOrder;

	public static CompanyImage of(Long companyId, CompanyImageType imageType, Long fileId, int sortOrder) {
		CompanyImage image = new CompanyImage();
		image.companyId = companyId;
		image.imageType = imageType;
		image.fileId = fileId;
		image.sortOrder = sortOrder;
		return image;
	}
}
