package com.example.packhub.domain.company.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 업체 목록 응답. 번호로 페이지를 넘긴다.
 *
 * @param page          지금 페이지 번호, 1부터 시작
 * @param totalElements 조건에 맞는 전체 업체 수
 * @param totalPages    전체 페이지 수
 */
public record CompanyPageResponse(
	List<CompanySummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {

	public static CompanyPageResponse of(Page<?> page, List<CompanySummaryResponse> content) {
		return new CompanyPageResponse(content, page.getNumber() + 1, page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}
}
