package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.company.entity.Company;

/**
 * 업체 목록의 카드 하나.
 *
 * @param logo 로고. 없으면 null
 */
public record CompanySummaryResponse(
	Long companyId,
	String name,
	String industry,
	String products,
	String address,
	CompanyFileResponse logo
) {

	public static CompanySummaryResponse of(Company company, CompanyFileResponse logo) {
		return new CompanySummaryResponse(company.getCompanyId(), company.getName(), company.getIndustry(),
			company.getProducts(), company.getAddress(), logo);
	}
}
