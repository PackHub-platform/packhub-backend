package com.example.packhub.domain.post.controller;

import com.example.packhub.domain.post.dto.MyPostPageResponse;
import com.example.packhub.domain.post.dto.MyPostSearchRequest;
import com.example.packhub.domain.post.dto.PostStatusChangeRequest;
import com.example.packhub.domain.post.dto.PostStatusResponse;
import com.example.packhub.domain.post.service.PostService;
import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시글 공통")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@Operation(summary = "게시글 마감·재게시 (9-2)",
		description = "모든 게시글 종류에 같이 쓴다. 작성자만 바꿀 수 있다. OPEN → CLOSED(마감), CLOSED → OPEN(재게시). "
			+ "지금과 같은 상태를 보내면 409다. 내 게시글 관리(10-1)의 \"마감/모집중\" 토글이 이 API다.")
	@PatchMapping("/posts/{postId}/status")
	public ApiResponse<PostStatusResponse> changeStatus(@CurrentMember Long memberId, @PathVariable Long postId,
		@Valid @RequestBody PostStatusChangeRequest request) {
		return ApiResponse.ok(postService.changeStatus(memberId, postId, request.status()));
	}

	@Operation(summary = "게시글 삭제 (5-5, 10-1)",
		description = "모든 게시글 종류에 같이 쓴다. 작성자만 삭제할 수 있다. 삭제한 글은 목록·상세·내 게시글에서 빠진다.")
	@DeleteMapping("/posts/{postId}")
	public ApiResponse<Void> delete(@CurrentMember Long memberId, @PathVariable Long postId) {
		postService.delete(memberId, postId);
		return ApiResponse.ok();
	}

	@Operation(summary = "내 게시글 목록 (10-1)",
		description = "내가 쓴 모든 종류의 게시글. 최근에 쓴 글 먼저. 삭제한 글은 빠지고, 관리자가 숨긴 글은 visibility가 HIDDEN이다. "
			+ "postType으로 탭을 나눌 수 있다. page는 1부터 시작한다.")
	@GetMapping("/members/me/posts")
	public ApiResponse<MyPostPageResponse> getMyPosts(@CurrentMember Long memberId,
		@ParameterObject @Valid @ModelAttribute MyPostSearchRequest request) {
		return ApiResponse.ok(postService.getMyPosts(memberId, request));
	}
}
