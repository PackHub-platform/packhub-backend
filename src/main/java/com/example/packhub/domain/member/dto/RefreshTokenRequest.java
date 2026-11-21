package com.example.packhub.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

/** 재발급·로그아웃에서 Refresh Token을 받는다 */
public record RefreshTokenRequest(
	@NotBlank String refreshToken
) {
}
