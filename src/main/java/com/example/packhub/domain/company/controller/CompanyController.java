package com.example.packhub.domain.company.controller;

import com.example.packhub.domain.company.dto.CompanyCreateRequest;
import com.example.packhub.domain.company.dto.CompanyCreateResponse;
import com.example.packhub.domain.company.service.CompanyService;
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

@Tag(name = "업체")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

	private final CompanyService companyService;

	@Operation(summary = "업체 등록 (4-2)",
		description = "이용권을 이용 중인 기업회원만 등록할 수 있고, 기업회원당 1개만 등록할 수 있다. "
			+ "파일은 파일 업로드 API로 먼저 올린 뒤 fileId를 보낸다.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CompanyCreateResponse> register(@CurrentMember Long memberId,
		@Valid @RequestBody CompanyCreateRequest request) {
		return ApiResponse.ok(companyService.register(memberId, request));
	}
}
