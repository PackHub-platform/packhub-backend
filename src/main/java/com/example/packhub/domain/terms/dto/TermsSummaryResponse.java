package com.example.packhub.domain.terms.dto;

import com.example.packhub.domain.terms.entity.Terms;
import com.example.packhub.domain.terms.entity.TermsType;
import java.time.LocalDateTime;

/** 약관 동의 체크박스 한 줄. 본문은 빼고 준다 */
public record TermsSummaryResponse(
	Long termsId,
	TermsType termsType,
	String title,
	String version,
	boolean isRequired,
	LocalDateTime effectiveAt
) {
	public static TermsSummaryResponse from(Terms terms) {
		return new TermsSummaryResponse(terms.getTermsId(), terms.getTermsType(), terms.getTitle(),
			terms.getVersion(), terms.isRequired(), terms.getEffectiveAt());
	}
}
