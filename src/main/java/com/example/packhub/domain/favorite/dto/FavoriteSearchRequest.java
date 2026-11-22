package com.example.packhub.domain.favorite.dto;

import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 관심 목록 조회 조건 (쿼리 파라미터). 모두 선택이다.
 *
 * @param targetType 대상 종류. 보내지 않으면 전체
 * @param page       페이지 번호, 1부터 시작 (기본 1)
 * @param size       한 페이지의 개수 (기본 20, 최대 50)
 */
public record FavoriteSearchRequest(
	FavoriteTargetType targetType,
	@Min(1) Integer page,
	@Min(1) @Max(50) Integer size
) {

	private static final int DEFAULT_SIZE = 20;

	public FavoriteSearchRequest {
		page = page == null ? 1 : page;
		size = size == null ? DEFAULT_SIZE : size;
	}
}
