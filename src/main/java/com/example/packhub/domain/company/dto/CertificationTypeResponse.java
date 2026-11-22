package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.company.entity.CertificationType;

/** 화면에는 label을 보여 주고, 업체 등록 요청에는 code를 보낸다 */
public record CertificationTypeResponse(
	CertificationType code,
	String label,
	String description
) {

	public static CertificationTypeResponse from(CertificationType type) {
		return new CertificationTypeResponse(type, type.getLabel(), type.getDescription());
	}
}
