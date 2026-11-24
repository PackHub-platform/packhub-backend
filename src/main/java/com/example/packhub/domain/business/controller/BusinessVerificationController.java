package com.example.packhub.domain.business.controller;

import com.example.packhub.domain.business.dto.BusinessVerificationRequest;
import com.example.packhub.domain.business.dto.BusinessVerificationResponse;
import com.example.packhub.domain.business.service.BusinessVerificationService;
import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사업자 인증")
@RestController
@RequestMapping("/api/v1/business-verifications")
@RequiredArgsConstructor
public class BusinessVerificationController {

	private final BusinessVerificationService businessVerificationService;

	@Operation(summary = "사업자 인증 신청 (1-4)",
		description = "기업회원만 신청할 수 있다. 중간 범위에서는 사업자번호 형식·검증 숫자만 확인하고 바로 승인(APPROVED)한다. "
			+ "사업자등록증은 파일 업로드 API에 business-certificate 용도로 먼저 올린 뒤 fileId를 보낸다.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<BusinessVerificationResponse> apply(@CurrentMember Long memberId,
		@Valid @RequestBody BusinessVerificationRequest request) {
		return ApiResponse.ok(businessVerificationService.apply(memberId, request));
	}

	@Operation(summary = "내 사업자 인증 상태 (1-4, 1-5)",
		description = "가장 최근 신청 1건. 신청한 적 없으면 404 BUSINESS_VERIFICATION_NOT_FOUND → 사업자 인증 화면으로 보낸다.")
	@GetMapping("/me")
	public ApiResponse<BusinessVerificationResponse> getMine(@CurrentMember Long memberId) {
		return ApiResponse.ok(businessVerificationService.getMine(memberId));
	}
}
