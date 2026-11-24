package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import java.time.LocalDateTime;

public record RequestCreateResponse(Long postId, PostStatus status, LocalDateTime createdAt) {

	public static RequestCreateResponse from(Post post) {
		return new RequestCreateResponse(post.getPostId(), post.getStatus(), post.getCreatedAt());
	}
}
