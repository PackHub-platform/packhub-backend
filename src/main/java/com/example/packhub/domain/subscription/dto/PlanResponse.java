package com.example.packhub.domain.subscription.dto;

import com.example.packhub.domain.subscription.entity.Plan;
import com.example.packhub.domain.subscription.entity.PlanCode;

public record PlanResponse(
	Long planId,
	PlanCode code,
	String name,
	String description,
	Long monthlyPrice,
	Long yearlyPrice,
	boolean isConsultOnly
) {

	public static PlanResponse from(Plan plan) {
		return new PlanResponse(plan.getPlanId(), plan.getCode(), plan.getName(), plan.getDescription(),
			plan.getMonthlyPrice(), plan.getYearlyPrice(), plan.isConsultOnly());
	}
}
