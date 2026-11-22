package com.example.packhub.domain.terms.dto;

import jakarta.validation.constraints.NotNull;

/** 약관 하나에 대한 동의 여부. termsId는 GET /terms로 받은 값을 그대로 보낸다 */
public record TermsAgreementRequest(
	@NotNull Long termsId,
	@NotNull Boolean agreed
) {
}
