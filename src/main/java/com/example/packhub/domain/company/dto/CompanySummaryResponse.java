package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.company.entity.Company;

/**
 * 업체 목록의 카드 하나.
 *
 * @param logo      로고. 없으면 null
 * @param favorited 로그인한 회원이 이 업체를 저장(관심)했는지. 로그인하지 않았으면 false
 */
public record CompanySummaryResponse(
	Long companyId,
	String name,
	String industry,
	String products,
	String address,
	CompanyFileResponse logo,
	boolean favorited
) {

	public static CompanySummaryResponse of(Company company, CompanyFileResponse logo, boolean favorited) {
		return new CompanySummaryResponse(company.getCompanyId(), company.getName(), company.getIndustry(),
			company.getProducts(), company.getAddress(), logo, favorited);
	}
}
