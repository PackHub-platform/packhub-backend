package com.example.packhub.domain.subscription.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 요청 때 PENDING(결제 대기) 주문으로 만들고, 결제 승인 때 구독·이용기간을 채워 PAID로 바꾼다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;

	@Column(nullable = false, length = 30, unique = true)
	private String orderNo;

	@Column(nullable = false)
	private Long memberId;

	private Long subscriptionId;

	@Column(nullable = false)
	private Long planId;

	@Column(nullable = false, length = 50)
	private String planNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BillingCycle billingCycle;

	@Column(nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentStatus status;

	private Long paymentMethodId;

	@Column(length = 50)
	private String methodSummary;

	@Column(length = 100)
	private String pgTransactionId;

	private LocalDate periodStart;

	private LocalDate periodEnd;

	private LocalDateTime paidAt;

	@Column(length = 255)
	private String failureReason;

	@Column(length = 500)
	private String receiptUrl;

	/** 결제 요청: 결제 대기 주문을 만든다. 금액은 플랜 가격으로 서버가 정하고, 이용권 이름은 이 시점 값을 남긴다(스냅샷). */
	public static Payment pending(String orderNo, Long memberId, Plan plan, BillingCycle billingCycle) {
		Payment payment = new Payment();
		payment.orderNo = orderNo;
		payment.memberId = memberId;
		payment.planId = plan.getPlanId();
		payment.planNameSnapshot = plan.getName();
		payment.billingCycle = billingCycle;
		payment.amount = plan.priceOf(billingCycle);
		payment.status = PaymentStatus.PENDING;
		return payment;
	}

	/** 결제 승인: 시작된 구독과 이용기간을 연결하고 결제 완료로 바꾼다. */
	public void complete(Subscription subscription, String methodSummary, LocalDateTime paidAt) {
		this.subscriptionId = subscription.getSubscriptionId();
		this.periodStart = subscription.getCurrentPeriodStart();
		this.periodEnd = subscription.getCurrentPeriodEnd();
		this.methodSummary = methodSummary;
		this.paidAt = paidAt;
		this.status = PaymentStatus.PAID;
	}

	public boolean isPending() {
		return status == PaymentStatus.PENDING;
	}
}
