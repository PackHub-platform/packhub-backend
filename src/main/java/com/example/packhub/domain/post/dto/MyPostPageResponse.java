package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import java.util.List;
import org.springframework.data.domain.Page;

/** 내 게시글 목록 응답. 번호로 페이지를 넘긴다 (page는 1부터) */
public record MyPostPageResponse(
	List<MyPostSummaryResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {

	public static MyPostPageResponse of(Page<Post> page) {
		return new MyPostPageResponse(page.getContent().stream().map(MyPostSummaryResponse::from).toList(),
			page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
