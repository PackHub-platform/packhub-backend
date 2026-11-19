package com.example.packhub.domain.subscription.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanCode code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 200)
	private String description;

	private Long monthlyPrice;

	private Long yearlyPrice;

	@Column(nullable = false)
	private boolean isConsultOnly;

	@Column(nullable = false)
	private boolean isActive;

	public Long priceOf(BillingCycle billingCycle) {
		return billingCycle == BillingCycle.MONTHLY ? monthlyPrice : yearlyPrice;
	}
}
