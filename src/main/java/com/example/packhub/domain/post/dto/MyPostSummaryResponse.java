package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.entity.PostType;
import com.example.packhub.domain.post.entity.PostVisibility;
import java.time.LocalDateTime;

/**
 * 내 게시글 표의 한 줄 (피그마 10-1: 유형 · 게시글 · 등록일 · 마감/모집중 · 관리).
 *
 * @param visibility HIDDEN이면 관리자가 숨긴 글
 */
public record MyPostSummaryResponse(
	Long postId,
	PostType postType,
	String title,
	PostStatus status,
	PostVisibility visibility,
	LocalDateTime createdAt,
	LocalDateTime closedAt
) {

	public static MyPostSummaryResponse from(Post post) {
		return new MyPostSummaryResponse(post.getPostId(), post.getPostType(), post.getTitle(), post.getStatus(),
			post.getVisibility(), post.getCreatedAt(), post.getClosedAt());
	}
}
