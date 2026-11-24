package com.example.packhub.domain.post.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 의뢰 목록 카드 하나 (피그마 5-1: 업종 · 제목 · 업체명 · 수량 · 희망 납기일).
 *
 * @param industry    작성 업체의 업종 (업체 등록 정보). 의뢰 글의 카테고리가 아니다
 * @param companyName 작성 업체의 업체명
 * @param favorited   로그인한 회원이 관심으로 저장했으면 true. 로그인하지 않았으면 false
 */
public record RequestSummaryResponse(
	Long postId,
	String title,
	String industry,
	String companyName,
	Integer quantity,
	String quantityUnit,
	LocalDate desiredDeliveryDate,
	LocalDateTime createdAt,
	boolean favorited
) {

	/** 조회 쿼리에서 쓴다. favorited는 서비스에서 채운다 */
	public RequestSummaryResponse(Long postId, String title, String industry, String companyName, Integer quantity,
		String quantityUnit, LocalDate desiredDeliveryDate, LocalDateTime createdAt) {
		this(postId, title, industry, companyName, quantity, quantityUnit, desiredDeliveryDate, createdAt, false);
	}

	public RequestSummaryResponse withFavorited(boolean favorited) {
		return new RequestSummaryResponse(postId, title, industry, companyName, quantity, quantityUnit,
			desiredDeliveryDate, createdAt, favorited);
	}
}
