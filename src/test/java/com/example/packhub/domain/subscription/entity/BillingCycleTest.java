package com.example.packhub.domain.subscription.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingCycleTest {

	@Test
	void 월_결제는_시작일부터_한_달_뒤_전날까지다() {
		assertThat(BillingCycle.MONTHLY.periodEnd(LocalDate.of(2026, 11, 19))).isEqualTo(LocalDate.of(2026, 12, 18));
	}

	@Test
	void 월말에_시작한_월_결제는_다음_달_말일_전날까지다() {
		// 1/31 + 1개월 = 2/28 이므로 2/27까지 이용하고 다음 결제일은 2/28 (기말에 정기결제 기준일 정책 확인 필요)
		assertThat(BillingCycle.MONTHLY.periodEnd(LocalDate.of(2027, 1, 31))).isEqualTo(LocalDate.of(2027, 2, 27));
	}

	@Test
	void 연_결제는_시작일부터_일_년_뒤_전날까지다() {
		assertThat(BillingCycle.YEARLY.periodEnd(LocalDate.of(2026, 11, 19))).isEqualTo(LocalDate.of(2027, 11, 18));
	}

	@Test
	void 윤년_2월_29일에_시작한_연_결제는_다음_해_2월_27일까지다() {
		assertThat(BillingCycle.YEARLY.periodEnd(LocalDate.of(2028, 2, 29))).isEqualTo(LocalDate.of(2029, 2, 27));
	}
}
