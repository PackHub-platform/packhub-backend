package com.example.packhub.domain.subscription.dto;

import com.example.packhub.domain.subscription.entity.BillingCycle;
import com.example.packhub.domain.subscription.entity.Payment;
import com.example.packhub.domain.subscription.entity.PaymentStatus;
import com.example.packhub.domain.subscription.entity.Plan;
import com.example.packhub.domain.subscription.entity.PlanCode;
import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MySubscriptionResponse(
	Long subscriptionId,
	SubscriptionStatus status,
	PlanSummary plan,
	BillingCycle billingCycle,
	Long price,
	Long nextBillingAmount,
	LocalDate currentPeriodStart,
	LocalDate currentPeriodEnd,
	LocalDate nextBillingDate,
	boolean autoRenew,
	PaymentMethodSummary paymentMethod,
	List<RecentPayment> recentPayments
) {

	/**
	 * 결제수단은 중간 범위에서 실제 카드 등록이 없어 항상 null이다.
	 * 다음 결제 금액은 자동결제가 꺼져 있으면(해지 예약) null이다.
	 */
	public static MySubscriptionResponse of(Subscription subscription, Plan plan, List<Payment> recentPayments) {
		Long price = plan.priceOf(subscription.getBillingCycle());
		return new MySubscriptionResponse(
			subscription.getSubscriptionId(),
			subscription.getStatus(),
			new PlanSummary(plan.getPlanId(), plan.getCode(), plan.getName()),
			subscription.getBillingCycle(),
			price,
			subscription.isAutoRenew() ? price : null,
			subscription.getCurrentPeriodStart(),
			subscription.getCurrentPeriodEnd(),
			subscription.getNextBillingDate(),
			subscription.isAutoRenew(),
			null,
			recentPayments.stream().map(RecentPayment::from).toList());
	}

	public record PlanSummary(Long planId, PlanCode code, String name) {
	}

	public record PaymentMethodSummary(String cardBrand, String cardLast4, String expiresYm) {
	}

	public record RecentPayment(
		Long paymentId,
		String orderNo,
		String planName,
		Long amount,
		PaymentStatus status,
		LocalDateTime paidAt
	) {

		static RecentPayment from(Payment payment) {
			return new RecentPayment(payment.getPaymentId(), payment.getOrderNo(), payment.getPlanNameSnapshot(),
				payment.getAmount(), payment.getStatus(), payment.getPaidAt());
		}
	}
}
