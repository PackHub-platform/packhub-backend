package com.example.packhub.domain.post.controller;

import com.example.packhub.domain.post.dto.RequestSaveRequest;
import com.example.packhub.domain.post.dto.RequestCreateResponse;
import com.example.packhub.domain.post.dto.RequestDetailResponse;
import com.example.packhub.domain.post.dto.RequestPageResponse;
import com.example.packhub.domain.post.dto.RequestSearchRequest;
import com.example.packhub.domain.post.dto.RequestUpdateResponse;
import com.example.packhub.domain.post.service.RequestService;
import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.CurrentMember;
import com.example.packhub.global.security.LoginMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "의뢰하기")
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

	private final RequestService requestService;

	@Operation(summary = "의뢰 등록 (5-3)",
		description = "업체를 등록한 기업회원만 등록할 수 있고, 등록하면 바로 게시(OPEN)된다. 첨부자료 말고 모든 칸이 필수다. "
			+ "첨부는 파일 업로드 API에 post-image(사진) 또는 post-document(문서) 용도로 먼저 올린 뒤 fileIds에 순서대로 보낸다. "
			+ "담당자 연락처는 보내지 않는다 (상세에서 작성자 회원정보를 보여 준다).")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RequestCreateResponse> create(@CurrentMember Long memberId,
		@Valid @RequestBody RequestSaveRequest request) {
		return ApiResponse.ok(requestService.create(memberId, request));
	}

	@Operation(summary = "의뢰 목록 조회 (5-1)",
		description = "로그인 없이 조회할 수 있다. 게시 중(OPEN)인 의뢰만 나오고 마감·숨김·삭제된 의뢰는 나오지 않는다. "
			+ "검색어는 의뢰 제목·업체명에서 찾는다. categoryId에 대분류를 보내면 그 아래 소분류 전체에서 찾는다. "
			+ "납기 정렬은 희망납기 기준이고, 희망납기가 지난 의뢰는 맨 뒤에 온다. page는 1부터 시작한다.")
	@GetMapping
	public ApiResponse<RequestPageResponse> search(@ParameterObject @Valid @ModelAttribute RequestSearchRequest request) {
		return ApiResponse.ok(requestService.search(request));
	}

	@Operation(summary = "의뢰 상세 조회 (5-2, 5-5)",
		description = "로그인 없이 조회할 수 있다. 마감된 의뢰도 볼 수 있고, 숨김·삭제된 의뢰는 404다. "
			+ "로그인해서 보면 내 글일 때 isMine이 true다. 개인정보 공개를 끈 의뢰는 본인이 아니면 contact 값이 null이다. "
			+ "첨부의 url은 1시간 동안만 열리는 조회용 주소다.")
	@GetMapping("/{postId}")
	public ApiResponse<RequestDetailResponse> getDetail(
		@Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember, @PathVariable Long postId) {
		return ApiResponse.ok(requestService.getDetail(postId, loginMember == null ? null : loginMember.memberId()));
	}

	@Operation(summary = "의뢰 수정 (5-5)",
		description = "작성자만 수정할 수 있다. 등록과 같은 값을 모두 보내면 전체를 바꾸고, fileIds에서 뺀 첨부는 지워진다. "
			+ "마감된 의뢰는 수정할 수 없다(409). 상태(마감·재게시)는 PATCH /api/v1/posts/{postId}/status로 바꾼다.")
	@PutMapping("/{postId}")
	public ApiResponse<RequestUpdateResponse> update(@CurrentMember Long memberId, @PathVariable Long postId,
		@Valid @RequestBody RequestSaveRequest request) {
		return ApiResponse.ok(requestService.update(memberId, postId, request));
	}
}
