package com.example.packhub.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationConfirmRequest(
	@NotBlank @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력해 주세요.") String phone,
	@NotBlank @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해 주세요.") String code
) {
}
