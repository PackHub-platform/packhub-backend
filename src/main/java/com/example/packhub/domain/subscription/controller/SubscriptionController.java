package com.example.packhub.domain.subscription.controller;

import com.example.packhub.domain.subscription.dto.ConfirmRequest;
import com.example.packhub.domain.subscription.dto.ConfirmResponse;
import com.example.packhub.domain.subscription.dto.MySubscriptionResponse;
import com.example.packhub.domain.subscription.dto.OrderRequest;
import com.example.packhub.domain.subscription.dto.OrderResponse;
import com.example.packhub.domain.subscription.service.SubscriptionService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO(인증 머지 후): X-Member-Id 헤더는 로그인 기능이 생기기 전까지 쓰는 임시 방식이다.
 * 인증이 붙으면 로그인한 회원 정보로 바꾸고, 기업회원·사업자 인증 권한 체크를 추가한다.
 */
@Tag(name = "이용권·결제")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	@Operation(summary = "이용권 결제 요청 (11-2)", description = "주문 정보만 계산해 돌려준다. 중간 범위에서는 저장하지 않는다.")
	@PostMapping
	public ApiResponse<OrderResponse> createOrder(@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody OrderRequest request) {
		return ApiResponse.ok(subscriptionService.createOrder(memberId, request));
	}

	@Operation(summary = "결제 승인 처리 (11-4)", description = "중간 범위에서는 PG 없이 항상 성공 처리한다.")
	@PostMapping("/confirm")
	public ApiResponse<ConfirmResponse> confirm(@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody ConfirmRequest request) {
		return ApiResponse.ok(subscriptionService.confirm(memberId, request));
	}

	@Operation(summary = "현재 이용권 조회 (11-5)", description = "이용 중인 이용권이 없으면 data가 없다.")
	@GetMapping("/me")
	public ApiResponse<MySubscriptionResponse> getMySubscription(@RequestHeader("X-Member-Id") Long memberId) {
		return ApiResponse.ok(subscriptionService.getMySubscription(memberId));
	}
}
