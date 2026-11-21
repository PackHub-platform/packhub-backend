package com.example.packhub.domain.category.dto;

import com.example.packhub.domain.category.entity.Category;
import java.util.List;

/** 대분류 하나와 그 아래 소분류 목록. 업체·게시글에는 소분류의 categoryId를 저장한다 */
public record CategoryResponse(
	Long categoryId,
	String name,
	List<Child> children
) {

	public record Child(Long categoryId, String name) {
	}

	public static CategoryResponse of(Category parent, List<Category> children) {
		return new CategoryResponse(parent.getCategoryId(), parent.getName(), children.stream()
			.map(child -> new Child(child.getCategoryId(), child.getName()))
			.toList());
	}
}
