package com.example.packhub.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmRequest(
	@NotNull @Pattern(regexp = "PH-\\d{8}-\\d{6}", message = "주문번호 형식이 올바르지 않습니다.") String orderNo
) {
}
