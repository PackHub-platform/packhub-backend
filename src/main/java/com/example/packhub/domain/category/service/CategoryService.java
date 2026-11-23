package com.example.packhub.domain.category.service;

import com.example.packhub.domain.category.dto.CategoryResponse;
import com.example.packhub.domain.category.entity.Category;
import com.example.packhub.domain.category.entity.CategoryDomain;
import com.example.packhub.domain.category.repository.CategoryRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;

	/** 대분류와 소분류를 각각 sortOrder 순서로 돌려준다 */
	public List<CategoryResponse> getCategories(CategoryDomain domain) {
		List<Category> categories = categoryRepository.findByDomainAndIsActiveTrueOrderBySortOrderAscCategoryIdAsc(domain);
		Map<Long, List<Category>> childrenByParentId = categories.stream()
			.filter(category -> !category.isParent())
			.collect(Collectors.groupingBy(Category::getParentId));

		return categories.stream()
			.filter(Category::isParent)
			.map(parent -> CategoryResponse.of(parent, childrenByParentId.getOrDefault(parent.getCategoryId(), List.of())))
			.toList();
	}

	public Optional<Category> find(Long categoryId) {
		return categoryRepository.findById(categoryId);
	}

	/**
	 * 목록 필터로 받은 분류를 소분류 ID 목록으로 바꾼다. 대분류면 그 아래 소분류 전체, 소분류면 자기 자신이다.
	 * 없는 분류거나 다른 사용처의 분류면 400 INVALID_CATEGORY
	 */
	public List<Long> findFilterIds(Long categoryId, CategoryDomain domain) {
		Category category = categoryRepository.findById(categoryId)
			.filter(found -> found.getDomain() == domain)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CATEGORY));
		if (!category.isParent()) {
			return List.of(categoryId);
		}
		return categoryRepository.findByParentId(categoryId).stream()
			.map(Category::getCategoryId)
			.toList();
	}

	/** 고를 수 없는 분류(없음, 대분류, 다른 사용처, 사용 중지)면 400 INVALID_CATEGORY */
	public void validateSelectable(Long categoryId, CategoryDomain domain) {
		categoryRepository.findById(categoryId)
			.filter(category -> category.isSelectableIn(domain))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CATEGORY));
	}
}
