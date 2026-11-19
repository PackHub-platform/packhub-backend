package com.example.packhub.domain.subscription.dto;

import com.example.packhub.domain.subscription.entity.BillingCycle;
import com.example.packhub.domain.subscription.entity.Payment;

public record OrderResponse(
	String orderNo,
	Long planId,
	String planName,
	BillingCycle billingCycle,
	Long amount
) {

	public static OrderResponse from(Payment order) {
		return new OrderResponse(order.getOrderNo(), order.getPlanId(), order.getPlanNameSnapshot(),
			order.getBillingCycle(), order.getAmount());
	}
}
