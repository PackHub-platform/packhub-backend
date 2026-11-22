package com.example.packhub.domain.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 업체 목록 조회 조건 (쿼리 파라미터). 모두 선택이고, 보내지 않으면 기본값을 쓴다.
 *
 * @param keyword    검색어. 업체명·업종·생산품목 중 하나에 포함되면 찾는다
 * @param categoryId 카테고리 ID. 대분류면 그 아래 소분류 전체, 소분류면 그 소분류만 찾는다
 * @param sort       정렬 (기본 LATEST)
 * @param page       페이지 번호, 1부터 시작 (기본 1)
 * @param size       한 페이지의 업체 수 (기본 24, 최대 50)
 */
public record CompanySearchRequest(
	@Size(max = 50) String keyword,
	Long categoryId,
	CompanySort sort,
	@Min(1) Integer page,
	@Min(1) @Max(50) Integer size
) {

	private static final int DEFAULT_SIZE = 24;

	public CompanySearchRequest {
		keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
		sort = sort == null ? CompanySort.LATEST : sort;
		page = page == null ? 1 : page;
		size = size == null ? DEFAULT_SIZE : size;
	}
}
