package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import java.time.LocalDateTime;

public record RequestUpdateResponse(Long postId, PostStatus status, LocalDateTime updatedAt) {

	public static RequestUpdateResponse of(Post post, LocalDateTime updatedAt) {
		return new RequestUpdateResponse(post.getPostId(), post.getStatus(), updatedAt);
	}
}
