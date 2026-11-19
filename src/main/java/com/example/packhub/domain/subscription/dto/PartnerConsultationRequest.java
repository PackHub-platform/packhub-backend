package com.example.packhub.domain.subscription.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PartnerConsultationRequest(
	@NotBlank @Size(max = 100) String companyName,
	@NotBlank @Size(max = 50) String managerName,
	@NotBlank @Size(max = 20) String phone,
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank String content,
	@AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다.") boolean privacyAgreed
) {
}
