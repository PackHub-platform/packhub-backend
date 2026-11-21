package com.example.packhub.domain.file.controller;

import com.example.packhub.domain.file.dto.FileRegisterRequest;
import com.example.packhub.domain.file.dto.FileResponse;
import com.example.packhub.domain.file.dto.PresignedUrlRequest;
import com.example.packhub.domain.file.dto.PresignedUrlResponse;
import com.example.packhub.domain.file.service.FileService;
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

@Tag(name = "파일")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

	private final FileService fileService;

	@Operation(summary = "파일 업로드 URL 발급 (9-3, 1단계)",
		description = "받은 uploadUrl로 S3에 직접 PUT한다. Content-Type과 파일 크기가 요청 값과 같아야 하고, URL은 5분간 유효하다.")
	@PostMapping("/presigned-url")
	public ApiResponse<PresignedUrlResponse> issueUploadUrl(@CurrentMember Long memberId,
		@Valid @RequestBody PresignedUrlRequest request) {
		return ApiResponse.ok(fileService.issueUploadUrl(memberId, request));
	}

	@Operation(summary = "파일 업로드 완료 등록 (9-3, 3단계)",
		description = "S3에 올라간 파일을 확인하고 fileId를 돌려준다. 같은 storageKey로 다시 보내면 처음 fileId를 돌려준다.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FileResponse> register(@CurrentMember Long memberId,
		@Valid @RequestBody FileRegisterRequest request) {
		return ApiResponse.ok(fileService.register(memberId, request));
	}
}
