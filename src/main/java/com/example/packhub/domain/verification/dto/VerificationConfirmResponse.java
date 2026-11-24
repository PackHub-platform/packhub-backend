package com.example.packhub.domain.verification.dto;

import java.time.LocalDateTime;

/** 인증 성공. 이 시각부터 30분 안에 가입을 마쳐야 한다 */
public record VerificationConfirmResponse(String target, boolean verified, LocalDateTime verifiedAt) {
}
