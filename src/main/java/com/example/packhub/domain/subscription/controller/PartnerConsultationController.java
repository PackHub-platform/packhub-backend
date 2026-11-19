package com.example.packhub.domain.subscription.controller;

import com.example.packhub.domain.subscription.dto.PartnerConsultationRequest;
import com.example.packhub.domain.subscription.service.PartnerConsultationService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** TODO(인증 머지 후): X-Member-Id 임시 헤더를 로그인한 회원 정보로 바꾼다. */
@Tag(name = "이용권·결제")
@RestController
@RequestMapping("/api/v1/partner-consultations")
@RequiredArgsConstructor
public class PartnerConsultationController {

	private final PartnerConsultationService partnerConsultationService;

	@Operation(summary = "파트너 상담 신청 (11-3)")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<Map<String, Long>> apply(@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody PartnerConsultationRequest request) {
		Long id = partnerConsultationService.apply(memberId, request);
		return ApiResponse.ok(Map.of("partnerConsultationId", id));
	}
}
