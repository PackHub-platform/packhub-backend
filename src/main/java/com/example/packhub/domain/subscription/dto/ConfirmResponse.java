package com.example.packhub.domain.subscription.dto;

import com.example.packhub.domain.subscription.entity.Payment;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConfirmResponse(
	String orderNo,
	String planName,
	Long amount,
	String methodSummary,
	LocalDate periodStart,
	LocalDate periodEnd,
	LocalDateTime paidAt
) {

	public static ConfirmResponse from(Payment payment) {
		return new ConfirmResponse(payment.getOrderNo(), payment.getPlanNameSnapshot(), payment.getAmount(),
			payment.getMethodSummary(), payment.getPeriodStart(), payment.getPeriodEnd(), payment.getPaidAt());
	}
}
