package com.example.packhub.domain.company.controller;

import com.example.packhub.domain.company.dto.CompanySaveRequest;
import com.example.packhub.domain.company.dto.CompanyCreateResponse;
import com.example.packhub.domain.company.dto.CompanyDetailResponse;
import com.example.packhub.domain.company.dto.CompanyPageResponse;
import com.example.packhub.domain.company.dto.CompanySearchRequest;
import com.example.packhub.domain.company.service.CompanyService;
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
		@Valid @RequestBody CompanySaveRequest request) {
		return ApiResponse.ok(companyService.register(memberId, request));
	}

	@Operation(summary = "업체 목록 조회 (4-1)",
		description = "로그인 없이 조회할 수 있다. 삭제했거나 숨김 상태인 업체는 나오지 않는다. "
			+ "categoryId에 대분류를 보내면 그 아래 소분류 전체에서 찾는다. page는 1부터 시작한다. "
			+ "로그인하고 조회하면 저장(관심)한 업체에 favorited가 true로 나온다.")
	@GetMapping
	public ApiResponse<CompanyPageResponse> search(
		@Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
		@ParameterObject @Valid @ModelAttribute CompanySearchRequest request) {
		return ApiResponse.ok(companyService.search(request, memberIdOf(loginMember)));
	}

	@Operation(summary = "업체 상세 조회 (4-3)",
		description = "로그인 없이 조회할 수 있다. 삭제했거나 숨김 상태인 업체는 404다. "
			+ "파일의 url은 1시간 동안만 열리는 조회용 주소다.")
	@GetMapping("/{companyId}")
	public ApiResponse<CompanyDetailResponse> getDetail(
		@Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember, @PathVariable Long companyId) {
		return ApiResponse.ok(companyService.getDetail(companyId, memberIdOf(loginMember)));
	}

	@Operation(summary = "내 업체 조회 (10-3)",
		description = "로그인한 기업회원의 업체. 숨김 상태여도 조회된다. 없으면 404다.")
	@GetMapping("/me")
	public ApiResponse<CompanyDetailResponse> getMine(@CurrentMember Long memberId) {
		return ApiResponse.ok(companyService.getMine(memberId));
	}

	@Operation(summary = "업체 정보 수정 (10-3)",
		description = "등록과 같은 값을 모두 보내면 전체를 바꾼다. 목록에서 뺀 항목은 지워진다.")
	@PutMapping("/me")
	public ApiResponse<CompanyDetailResponse> update(@CurrentMember Long memberId,
		@Valid @RequestBody CompanySaveRequest request) {
		return ApiResponse.ok(companyService.update(memberId, request));
	}

	/** 목록·상세는 로그인 없이도 볼 수 있어서 회원이 없을 수 있다. 로그인하지 않았으면 null */
	private Long memberIdOf(LoginMember loginMember) {
		return loginMember == null ? null : loginMember.memberId();
	}
}
