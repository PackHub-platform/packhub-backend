package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.MemberType;

/**
 * 가입하자마자 로그인 상태로 다음 단계를 이어가도록 토큰을 함께 준다.
 * refreshToken은 재발급·로그아웃 이슈에서 추가한다.
 */
public record SignUpResponse(
	Long memberId,
	MemberType memberType,
	String email,
	SignUpNextStep nextStep,
	String accessToken,
	String tokenType,
	long accessTokenExpiresIn
) {
}
