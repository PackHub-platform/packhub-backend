package com.example.packhub.domain.favorite.dto;

import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 관심 목록의 카드 하나 (10-2).
 *
 * @param targetId 대상의 ID. 업체면 companyId, 게시글이면 postId다
 * @param title    업체면 업체명, 게시글이면 제목
 * @param content  업체면 회사설명, 게시글이면 내용
 * @param date     저장한 날짜
 */
public record FavoriteResponse(
	FavoriteTargetType targetType,
	Long targetId,
	String title,
	String content,
	LocalDate date
) {

	/** 조회 쿼리에서 쓴다. 저장한 시각에서 날짜만 남긴다 */
	public FavoriteResponse(FavoriteTargetType targetType, Long targetId, String title, String content,
		LocalDateTime favoritedAt) {
		this(targetType, targetId, title, content, favoritedAt.toLocalDate());
	}
}
