package com.example.packhub.domain.post.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 의뢰 목록 응답. 번호로 페이지를 넘긴다 (업체 목록과 같은 규칙).
 *
 * @param page          지금 페이지 번호, 1부터 시작
 * @param totalElements 조건에 맞는 전체 의뢰 수
 * @param totalPages    전체 페이지 수
 */
public record RequestPageResponse(
	List<RequestSummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {

	public static RequestPageResponse of(Page<RequestSummaryResponse> page) {
		return new RequestPageResponse(page.getContent(), page.getNumber() + 1, page.getSize(),
			page.getTotalElements(), page.getTotalPages());
	}
}
