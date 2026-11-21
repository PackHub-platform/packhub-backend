package com.example.packhub.domain.category.entity;

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
 * 대분류·소분류 2단 분류. 대분류는 parentId가 없고, 소분류는 parentId로 대분류를 가리킨다.
 * 목록은 마이그레이션으로 넣는다 (기초 데이터).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long categoryId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CategoryDomain domain;

	private Long parentId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean isActive;

	public boolean isParent() {
		return parentId == null;
	}

	/** 업체·게시글에는 해당 사용처의 사용 중인 소분류만 저장할 수 있다 (대분류는 고를 수 없다) */
	public boolean isSelectableIn(CategoryDomain domain) {
		return this.domain == domain && !isParent() && isActive;
	}
}
