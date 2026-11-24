package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import java.time.LocalDateTime;

/** @param closedAt 마감했으면 마감 시각, 다시 게시했으면 null */
public record PostStatusResponse(Long postId, PostStatus status, LocalDateTime closedAt) {

	public static PostStatusResponse from(Post post) {
		return new PostStatusResponse(post.getPostId(), post.getStatus(), post.getClosedAt());
	}
}
