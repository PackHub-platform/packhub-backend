package com.example.packhub.domain.post.controller;

import com.example.packhub.domain.post.dto.RequestCreateRequest;
import com.example.packhub.domain.post.dto.RequestCreateResponse;
import com.example.packhub.domain.post.service.RequestService;
import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
		@Valid @RequestBody RequestCreateRequest request) {
		return ApiResponse.ok(requestService.create(memberId, request));
	}
}
