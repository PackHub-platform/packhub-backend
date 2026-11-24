package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import java.time.LocalDateTime;

public record RequestUpdateResponse(Long postId, PostStatus status, LocalDateTime updatedAt) {

	public static RequestUpdateResponse from(Post post) {
		return new RequestUpdateResponse(post.getPostId(), post.getStatus(), post.getUpdatedAt());
	}
}
