package com.example.packhub.domain.member.dto;

/** 가입 직후 프론트가 보여 줄 다음 화면 */
public enum SignUpNextStep {
	/** 기업회원: 사업자 인증 화면 */
	BUSINESS_VERIFICATION,
	/** 일반회원: 가입 완료 화면 */
	DONE
}
