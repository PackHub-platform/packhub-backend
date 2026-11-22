package com.example.packhub.domain.terms.entity;

import java.util.List;

/**
 * 어느 화면에서 동의받는 약관인지. Terms 테이블에 칸을 두지 않고 여기서 약관 종류와 짝짓는다.
 * 약관 종류 자체가 코드의 고정 목록이라, 짝도 같은 곳에 둔다.
 */
public enum TermsUsage {
	SIGNUP(List.of(TermsType.SERVICE, TermsType.PRIVACY, TermsType.AGE_14, TermsType.LOCATION, TermsType.MARKETING)),
	PAYMENT(List.of(TermsType.PAYMENT_REFUND, TermsType.RECURRING_PAYMENT)),
	INQUIRY(List.of(TermsType.INQUIRY_PRIVACY)),
	CONSULT(List.of(TermsType.CONSULT_PRIVACY));

	/** 화면에 보여 줄 순서 */
	private final List<TermsType> termsTypes;

	TermsUsage(List<TermsType> termsTypes) {
		this.termsTypes = termsTypes;
	}

	public List<TermsType> getTermsTypes() {
		return termsTypes;
	}
}
