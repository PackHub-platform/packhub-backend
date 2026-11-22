package com.example.packhub.domain.terms.dto;

import com.example.packhub.domain.terms.entity.Terms;
import com.example.packhub.domain.terms.entity.TermsType;
import java.time.LocalDateTime;

public record TermsDetailResponse(
	Long termsId,
	TermsType termsType,
	String title,
	String version,
	String content,
	boolean isRequired,
	LocalDateTime effectiveAt
) {
	public static TermsDetailResponse from(Terms terms) {
		return new TermsDetailResponse(terms.getTermsId(), terms.getTermsType(), terms.getTitle(),
			terms.getVersion(), terms.getContent(), terms.isRequired(), terms.getEffectiveAt());
	}
}
