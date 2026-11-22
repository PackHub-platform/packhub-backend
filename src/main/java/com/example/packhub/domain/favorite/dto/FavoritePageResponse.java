package com.example.packhub.domain.favorite.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 관심 목록 응답. 번호로 페이지를 넘긴다.
 *
 * @param page          지금 페이지 번호, 1부터 시작
 * @param totalElements 전체 관심 수
 * @param totalPages    전체 페이지 수
 */
public record FavoritePageResponse(
	List<FavoriteResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {

	public static FavoritePageResponse of(Page<FavoriteResponse> page) {
		return new FavoritePageResponse(page.getContent(), page.getNumber() + 1, page.getSize(),
			page.getTotalElements(), page.getTotalPages());
	}
}
