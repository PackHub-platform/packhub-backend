package com.example.packhub.domain.subscription.controller;

import com.example.packhub.domain.subscription.dto.PlanResponse;
import com.example.packhub.domain.subscription.service.SubscriptionService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이용권·결제")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

	private final SubscriptionService subscriptionService;

	@Operation(summary = "플랜 목록 조회 (11-1)")
	@GetMapping
	public ApiResponse<List<PlanResponse>> getPlans() {
		return ApiResponse.ok(subscriptionService.getPlans());
	}
}
