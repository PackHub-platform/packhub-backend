package com.example.packhub.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 휴대폰 번호는 가입과 같은 형식(하이픈 없이 숫자만) */
public record PhoneVerificationRequest(
	@NotBlank @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력해 주세요.") String phone
) {
}
