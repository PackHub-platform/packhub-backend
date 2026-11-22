package com.example.packhub.domain.terms.controller;

import com.example.packhub.domain.terms.dto.TermsDetailResponse;
import com.example.packhub.domain.terms.dto.TermsSummaryResponse;
import com.example.packhub.domain.terms.entity.TermsType;
import com.example.packhub.domain.terms.entity.TermsUsage;
import com.example.packhub.domain.terms.service.TermsService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/terms/** 는 SecurityConfig에서 로그인 없이 열려 있다 */
@Tag(name = "약관")
@SecurityRequirements // Swagger에서 토큰 없이 호출한다고 표시
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

	private final TermsService termsService;

	@Operation(summary = "용도별 약관 목록 조회 (1-2)",
		description = "약관마다 시행 중인 최신 버전 하나만 준다. 받은 termsId를 그대로 회원가입 agreements에 보낸다.")
	@GetMapping
	public ApiResponse<List<TermsSummaryResponse>> getTermsList(
		@Parameter(description = "SIGNUP(기본값), PAYMENT, INQUIRY, CONSULT")
		@RequestParam(defaultValue = "SIGNUP") TermsUsage usage) {
		return ApiResponse.ok(termsService.getTermsList(usage));
	}

	@Operation(summary = "약관 본문 조회 (13-6)",
		description = "이용약관 페이지는 SERVICE, 개인정보처리방침 페이지는 PRIVACY로 호출한다. version이 없으면 시행 중인 최신 버전을 준다.")
	@GetMapping("/{termsType}")
	public ApiResponse<TermsDetailResponse> getTerms(
		@PathVariable TermsType termsType,
		@Parameter(description = "특정 버전 조회 (예: 1.0)") @RequestParam(required = false) String version) {
		return ApiResponse.ok(termsService.getTerms(termsType, version));
	}
}
