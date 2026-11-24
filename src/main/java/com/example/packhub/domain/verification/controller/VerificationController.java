package com.example.packhub.domain.verification.controller;

import com.example.packhub.domain.verification.dto.EmailVerificationConfirmRequest;
import com.example.packhub.domain.verification.dto.EmailVerificationRequest;
import com.example.packhub.domain.verification.dto.PhoneVerificationConfirmRequest;
import com.example.packhub.domain.verification.dto.PhoneVerificationRequest;
import com.example.packhub.domain.verification.dto.VerificationConfirmResponse;
import com.example.packhub.domain.verification.dto.VerificationSendResponse;
import com.example.packhub.domain.verification.entity.VerificationChannel;
import com.example.packhub.domain.verification.service.VerificationService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증번호")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class VerificationController {

	private static final String SEND_DESCRIPTION = "인증번호 6자리를 보낸다(5분 유효, 60초 뒤 재발송). 이미 가입된 값이면 409다. "
		+ "문자·메일 발송이 정해지기 전이라 서버 로그로 출력하고, 로컬·개발 서버에서는 응답 code에도 넣어 준다.";
	private static final String CONFIRM_DESCRIPTION = "가장 최근에 보낸 인증번호와 맞춰 본다. 5회 틀리면 다시 발송해야 한다. "
		+ "성공하면 30분 안에 가입을 마쳐야 한다.";

	private final VerificationService verificationService;

	@Operation(summary = "이메일 인증번호 발송 (1-3)", description = SEND_DESCRIPTION)
	@PostMapping("/email-verifications")
	public ApiResponse<VerificationSendResponse> sendEmail(@Valid @RequestBody EmailVerificationRequest request) {
		return ApiResponse.ok(verificationService.send(VerificationChannel.EMAIL, request.email()));
	}

	@Operation(summary = "이메일 인증번호 확인 (1-3)", description = CONFIRM_DESCRIPTION)
	@PostMapping("/email-verifications/confirm")
	public ApiResponse<VerificationConfirmResponse> confirmEmail(
		@Valid @RequestBody EmailVerificationConfirmRequest request) {
		return ApiResponse.ok(verificationService.confirm(VerificationChannel.EMAIL, request.email(), request.code()));
	}

	@Operation(summary = "휴대폰 인증번호 발송 (1-3)", description = SEND_DESCRIPTION)
	@PostMapping("/phone-verifications")
	public ApiResponse<VerificationSendResponse> sendPhone(@Valid @RequestBody PhoneVerificationRequest request) {
		return ApiResponse.ok(verificationService.send(VerificationChannel.PHONE, request.phone()));
	}

	@Operation(summary = "휴대폰 인증번호 확인 (1-3)", description = CONFIRM_DESCRIPTION)
	@PostMapping("/phone-verifications/confirm")
	public ApiResponse<VerificationConfirmResponse> confirmPhone(
		@Valid @RequestBody PhoneVerificationConfirmRequest request) {
		return ApiResponse.ok(verificationService.confirm(VerificationChannel.PHONE, request.phone(), request.code()));
	}
}
