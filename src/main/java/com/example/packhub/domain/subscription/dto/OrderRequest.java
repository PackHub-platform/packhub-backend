package com.example.packhub.domain.subscription.dto;

import com.example.packhub.domain.subscription.entity.BillingCycle;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
	@NotNull Long planId,
	@NotNull BillingCycle billingCycle,
	@AssertTrue(message = "이용약관·환불 기준에 동의해야 합니다.") boolean agreePaymentTerms,
	@AssertTrue(message = "정기결제에 동의해야 합니다.") boolean agreeRecurringPayment
) {
}
