package com.example.packhub.domain.post.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 의뢰 목록 카드 하나 (피그마 5-1: 업종 · 제목 · 업체명 · 수량 · 희망 납기일).
 *
 * @param industry    작성 업체의 업종 (업체 등록 정보). 의뢰 글의 카테고리가 아니다
 * @param companyName 작성 업체의 업체명
 */
public record RequestSummaryResponse(
	Long postId,
	String title,
	String industry,
	String companyName,
	Integer quantity,
	String quantityUnit,
	LocalDate desiredDeliveryDate,
	LocalDateTime createdAt
) {
}
