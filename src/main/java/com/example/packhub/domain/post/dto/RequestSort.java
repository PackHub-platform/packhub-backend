package com.example.packhub.domain.post.dto;

/**
 * 의뢰 목록 정렬 (피그마 5-1). 납기 정렬은 희망납기 기준이고, 희망납기가 지난 의뢰는 맨 뒤로 보낸다.
 * 두 규칙 모두 피그마 설명 글상자에 "확인 필요"로 적힌 임시 기준이다 (2026-11-24, PM 확인 중).
 */
public enum RequestSort {
	/** 최신순: 최근에 등록한 의뢰 먼저 */
	LATEST,
	/** 납기 임박순: 희망납기가 가까운 의뢰 먼저 */
	DELIVERY_ASC,
	/** 납기 여유순: 희망납기가 먼 의뢰 먼저 */
	DELIVERY_DESC
}
