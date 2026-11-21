package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.company.entity.CertificationType;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyCertification;
import com.example.packhub.domain.company.entity.CompanyEquipment;
import com.example.packhub.domain.company.entity.CompanyImage;
import com.example.packhub.domain.company.entity.CompanyImageType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 업체 상세 (4-3). 목록은 등록할 때 보낸 순서(sortOrder)대로다.
 * 로고·배경사진은 없으면 null이고, 포트폴리오는 PDF일 수도 있다 (contentType으로 구분).
 */
public record CompanyDetailResponse(
	Long companyId,
	String name,
	String industry,
	String products,
	Category category,
	String businessHours,
	String contactPhone,
	String address,
	String addressDetail,
	BigDecimal latitude,
	BigDecimal longitude,
	String representativeName,
	LocalDate foundedDate,
	Integer employeeCount,
	String homepageUrl,
	String description,
	CompanyFileResponse logo,
	CompanyFileResponse background,
	List<Certification> certifications,
	List<Equipment> equipments,
	List<CompanyFileResponse> portfolios,
	List<CompanyFileResponse> officeImages
) {

	/** 업체가 고른 소분류와 그 대분류 */
	public record Category(Long categoryId, String name, Long parentId, String parentName) {
	}

	public record Certification(CertificationType code, String label, String description) {
	}

	public record Equipment(String name, CompanyFileResponse image) {
	}

	/** @param files fileId로 찾을 수 있게 모아 둔 파일 응답. 삭제된 파일은 들어 있지 않다 */
	public static CompanyDetailResponse of(Company company, Category category,
		List<CompanyCertification> certifications, List<CompanyEquipment> equipments, List<CompanyImage> images,
		Map<Long, CompanyFileResponse> files) {
		return new CompanyDetailResponse(
			company.getCompanyId(),
			company.getName(),
			company.getIndustry(),
			company.getProducts(),
			category,
			company.getBusinessHours(),
			company.getContactPhone(),
			company.getAddress(),
			company.getAddressDetail(),
			company.getLatitude(),
			company.getLongitude(),
			company.getRepresentativeName(),
			company.getFoundedDate(),
			company.getEmployeeCount(),
			company.getHomepageUrl(),
			company.getDescription(),
			files.get(company.getLogoFileId()),
			files.get(company.getBackgroundFileId()),
			certifications.stream()
				.map(CompanyCertification::getCertType)
				.map(type -> new Certification(type, type.getLabel(), type.getDescription()))
				.toList(),
			equipments.stream()
				.map(equipment -> new Equipment(equipment.getName(), files.get(equipment.getImageFileId())))
				.toList(),
			imagesOf(images, CompanyImageType.PORTFOLIO, files),
			imagesOf(images, CompanyImageType.OFFICE, files));
	}

	private static List<CompanyFileResponse> imagesOf(List<CompanyImage> images, CompanyImageType imageType,
		Map<Long, CompanyFileResponse> files) {
		return images.stream()
			.filter(image -> image.getImageType() == imageType)
			.map(image -> files.get(image.getFileId()))
			.filter(Objects::nonNull)
			.toList();
	}
}
