package com.example.packhub.domain.verification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해 주세요.") String code
) {
}
