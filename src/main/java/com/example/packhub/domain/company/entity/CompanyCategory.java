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

/**
 * 업체가 고른 카테고리(소분류). 대분류는 Category.parentId로 알 수 있어 저장하지 않는다.
 * 지금은 업체당 1개만 저장한다. 여러 개를 허용하게 되면 행만 늘리면 된다 (논의 예정).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyCategory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long companyCategoryId;

	@Column(nullable = false)
	private Long companyId;

	@Column(nullable = false)
	private Long categoryId;

	public static CompanyCategory of(Long companyId, Long categoryId) {
		CompanyCategory companyCategory = new CompanyCategory();
		companyCategory.companyId = companyId;
		companyCategory.categoryId = categoryId;
		return companyCategory;
	}
}
