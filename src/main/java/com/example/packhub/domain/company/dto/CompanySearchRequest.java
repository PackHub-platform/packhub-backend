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
 * @param page       페이지 번호, 1부터 시작 (기본 1, 최대 100000)
 * @param size       한 페이지의 업체 수 (기본 24, 최대 50)
 */
public record CompanySearchRequest(
	@Size(max = 50) String keyword,
	Long categoryId,
	CompanySort sort,
	@Min(1) @Max(MAX_PAGE) Integer page,
	@Min(1) @Max(50) Integer size
) {

	/** 이보다 큰 페이지 번호는 400. 건너뛸 행 수(page × size)가 int 범위를 넘으면 조회가 실패하기 때문에 막는다 */
	private static final int MAX_PAGE = 100_000;
	private static final int DEFAULT_SIZE = 24;

	public CompanySearchRequest {
		keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
		sort = sort == null ? CompanySort.LATEST : sort;
		page = page == null ? 1 : page;
		size = size == null ? DEFAULT_SIZE : size;
	}
}
