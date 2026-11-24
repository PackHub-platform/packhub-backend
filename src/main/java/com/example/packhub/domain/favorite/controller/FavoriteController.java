package com.example.packhub.domain.favorite.controller;

import com.example.packhub.domain.favorite.dto.FavoritePageResponse;
import com.example.packhub.domain.favorite.dto.FavoriteSearchRequest;
import com.example.packhub.domain.favorite.service.FavoriteService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class FavoriteController {

	private final FavoriteService favoriteService;

	@Operation(summary = "업체 관심 등록 (9-1)",
		description = "이미 저장한 업체를 다시 보내도 성공이다. 없거나 숨김·삭제된 업체는 404다.")
	@PutMapping("/companies/{companyId}")
	public ApiResponse<Void> addCompany(@CurrentMember Long memberId, @PathVariable Long companyId) {
		favoriteService.addCompany(memberId, companyId);
		return ApiResponse.ok();
	}

	@Operation(summary = "업체 관심 해제 (9-1)", description = "저장하지 않은 업체를 보내도 성공이다.")
	@DeleteMapping("/companies/{companyId}")
	public ApiResponse<Void> removeCompany(@CurrentMember Long memberId, @PathVariable Long companyId) {
		favoriteService.removeCompany(memberId, companyId);
		return ApiResponse.ok();
	}

	@Operation(summary = "게시글 관심 등록 (9-1)",
		description = "이미 저장한 게시글을 다시 보내도 성공이다. 마감된 글도 저장할 수 있고, 없거나 숨김·삭제된 글은 404다.")
	@PutMapping("/posts/{postId}")
	public ApiResponse<Void> addPost(@CurrentMember Long memberId, @PathVariable Long postId) {
		favoriteService.addPost(memberId, postId);
		return ApiResponse.ok();
	}

	@Operation(summary = "게시글 관심 해제 (9-1)", description = "저장하지 않은 게시글을 보내도 성공이다.")
	@DeleteMapping("/posts/{postId}")
	public ApiResponse<Void> removePost(@CurrentMember Long memberId, @PathVariable Long postId) {
		favoriteService.removePost(memberId, postId);
		return ApiResponse.ok();
	}

	@Operation(summary = "관심 목록 조회 (10-2)",
		description = "최근에 저장한 순서다. 저장한 뒤 숨김·삭제된 업체·게시글은 나오지 않는다. "
			+ "targetType(COMPANY, POST)을 보내지 않으면 업체와 게시글이 섞여 나온다. page는 1부터 시작한다.")
	@GetMapping
	public ApiResponse<FavoritePageResponse> getFavorites(@CurrentMember Long memberId,
		@ParameterObject @Valid @ModelAttribute FavoriteSearchRequest request) {
		return ApiResponse.ok(favoriteService.getFavorites(memberId, request));
	}
}
