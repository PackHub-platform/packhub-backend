package com.example.packhub.domain.favorite.dto;

import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 관심 목록 조회 조건 (쿼리 파라미터). 모두 선택이다.
 *
 * @param targetType 대상 종류. 보내지 않으면 전체
 * @param page       페이지 번호, 1부터 시작 (기본 1, 최대 100000)
 * @param size       한 페이지의 개수 (기본 20, 최대 50)
 */
public record FavoriteSearchRequest(
	FavoriteTargetType targetType,
	@Min(1) @Max(MAX_PAGE) Integer page,
	@Min(1) @Max(50) Integer size
) {

	/** 이보다 큰 페이지 번호는 400. 건너뛸 행 수(page × size)가 int 범위를 넘으면 조회가 실패하기 때문에 막는다 */
	private static final int MAX_PAGE = 100_000;
	private static final int DEFAULT_SIZE = 20;

	public FavoriteSearchRequest {
		page = page == null ? 1 : page;
		size = size == null ? DEFAULT_SIZE : size;
	}
}
