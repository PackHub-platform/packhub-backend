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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long subscriptionId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Long planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SubscriptionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GrantType grantType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BillingCycle billingCycle;

	@Column(nullable = false)
	private LocalDate currentPeriodStart;

	@Column(nullable = false)
	private LocalDate currentPeriodEnd;

	private LocalDate nextBillingDate;

	@Column(nullable = false)
	private boolean autoRenew;

	private Long paymentMethodId;

	private Long pendingPlanId;

	private LocalDateTime cancelRequestedAt;

	private LocalDate freeUntil;

	/** 유료 결제로 이용권을 시작한다. 이용기간은 시작일부터 1개월(또는 1년)이고, 다음 결제일은 끝난 다음 날이다. */
	public static Subscription startPaid(Long memberId, Long planId, BillingCycle billingCycle, LocalDate start) {
		Subscription subscription = new Subscription();
		subscription.memberId = memberId;
		subscription.planId = planId;
		subscription.status = SubscriptionStatus.ACTIVE;
		subscription.grantType = GrantType.PAID;
		subscription.billingCycle = billingCycle;
		subscription.currentPeriodStart = start;
		subscription.currentPeriodEnd = billingCycle.periodEnd(start);
		subscription.nextBillingDate = subscription.currentPeriodEnd.plusDays(1);
		subscription.autoRenew = true;
		return subscription;
	}
}
