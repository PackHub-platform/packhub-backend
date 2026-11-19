package com.example.packhub.domain.subscription.entity;

import java.time.LocalDate;

public enum BillingCycle {
	MONTHLY,
	YEARLY;

	/** 시작일부터 한 주기의 마지막 날. 예: 11/19 시작 월 결제 → 12/18 */
	public LocalDate periodEnd(LocalDate start) {
		LocalDate next = this == MONTHLY ? start.plusMonths(1) : start.plusYears(1);
		return next.minusDays(1);
	}
}
