package com.example.packhub.domain.business.dto;

import com.example.packhub.domain.business.entity.BusinessVerification;
import com.example.packhub.domain.business.entity.BusinessVerificationStatus;
import java.time.LocalDateTime;

public record BusinessVerificationResponse(
	Long businessVerificationId,
	String businessName,
	String representativeName,
	String businessNumber,
	BusinessVerificationStatus status,
	String rejectReason,
	LocalDateTime createdAt,
	LocalDateTime reviewedAt
) {

	public static BusinessVerificationResponse from(BusinessVerification verification) {
		return new BusinessVerificationResponse(verification.getBusinessVerificationId(),
			verification.getBusinessName(), verification.getRepresentativeName(), verification.getBusinessNumber(),
			verification.getStatus(), verification.getRejectReason(), verification.getCreatedAt(),
			verification.getReviewedAt());
	}
}
