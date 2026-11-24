package com.example.packhub.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * 인증번호 발송 결과.
 *
 * @param target            인증번호를 보낸 이메일 또는 휴대폰 번호
 * @param expiresAt         이 시각까지 입력해야 한다 (5분)
 * @param resendAvailableAt 이 시각부터 다시 보낼 수 있다 (60초)
 * @param code              로컬·개발 서버에서만 들어가는 인증번호 (문자·메일 발송이 없어서 프론트·시연용). 운영에서는 빠진다
 */
public record VerificationSendResponse(
	String target,
	LocalDateTime expiresAt,
	LocalDateTime resendAvailableAt,
	@JsonInclude(JsonInclude.Include.NON_NULL) String code
) {
}
