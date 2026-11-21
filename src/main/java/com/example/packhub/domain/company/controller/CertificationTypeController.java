package com.example.packhub.domain.company.controller;

import com.example.packhub.domain.company.dto.CertificationTypeResponse;
import com.example.packhub.domain.company.entity.CertificationType;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "업체")
@RestController
@RequestMapping("/api/v1/certification-types")
public class CertificationTypeController {

	@Operation(summary = "보유 인증 목록 조회",
		description = "업체 등록과 업체 검색 필터에서 고를 수 있는 인증 목록. 로그인 없이 조회할 수 있다.")
	@GetMapping
	public ApiResponse<List<CertificationTypeResponse>> getCertificationTypes() {
		return ApiResponse.ok(Arrays.stream(CertificationType.values())
			.map(CertificationTypeResponse::from)
			.toList());
	}
}
