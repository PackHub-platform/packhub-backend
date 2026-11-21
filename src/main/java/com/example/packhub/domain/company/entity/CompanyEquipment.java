package com.example.packhub.domain.company.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 보유 설비 (최대 20개). 설비명과 사진을 둘 다 받는다. DB는 사진 NULL을 허용하지만 요청 검증에서 필수로 받는다 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyEquipment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long companyEquipmentId;

	@Column(nullable = false)
	private Long companyId;

	@Column(nullable = false, length = 100)
	private String name;

	private Long imageFileId;

	@Column(nullable = false)
	private int sortOrder;

	public static CompanyEquipment of(Long companyId, String name, Long imageFileId, int sortOrder) {
		CompanyEquipment equipment = new CompanyEquipment();
		equipment.companyId = companyId;
		equipment.name = name;
		equipment.imageFileId = imageFileId;
		equipment.sortOrder = sortOrder;
		return equipment;
	}
}
