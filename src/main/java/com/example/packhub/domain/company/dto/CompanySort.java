package com.example.packhub.domain.company.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * 업체 목록 정렬. 값이 같은 업체의 순서가 페이지마다 흔들리지 않도록 companyId를 두 번째 기준으로 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum CompanySort {

	/** 최신순: 최근에 등록한 업체 먼저 */
	LATEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("companyId"))),
	/** 개업일순: 설립일이 오래된 업체 먼저 */
	FOUNDED(Sort.by(Sort.Order.asc("foundedDate"), Sort.Order.desc("companyId"))),
	/** 규모순: 직원 수가 많은 업체 먼저 */
	SIZE(Sort.by(Sort.Order.desc("employeeCount"), Sort.Order.desc("companyId")));

	private final Sort sort;
}
