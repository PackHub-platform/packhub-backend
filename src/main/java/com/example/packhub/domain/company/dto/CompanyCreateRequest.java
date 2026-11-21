package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.company.entity.CertificationType;
import com.example.packhub.domain.company.entity.Company;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 업체 등록 요청. 필수값은 ERD의 NOT NULL을 따른다.
 * 파일은 먼저 파일 업로드 API로 올리고 여기에는 fileId만 담는다. 목록의 순서가 화면 표시 순서다.
 *
 * @param categoryId       카테고리 소분류 ID (GET /api/v1/categories?domain=COMMON). 지금은 1개만 받는다
 * @param certTypes        보유 인증 코드 목록 (GET /api/v1/certification-types). 없으면 빈 배열
 * @param portfolioFileIds 포트폴리오 파일 (용도 COMPANY_PORTFOLIO, PDF 가능)
 * @param officeFileIds    업체 사진 (용도 COMPANY_IMAGE)
 */
public record CompanyCreateRequest(
	@NotBlank @Size(max = 100) String name,
	@NotBlank @Size(max = 50) String industry,
	@NotBlank @Size(max = 255) String products,
	@NotNull Long categoryId,
	@NotBlank @Size(max = 50) String businessHours,
	@NotBlank @Size(max = 20) String contactPhone,
	@NotBlank @Size(max = 255) String address,
	@Size(max = 255) String addressDetail,
	@Digits(integer = 3, fraction = 7) BigDecimal latitude,
	@Digits(integer = 3, fraction = 7) BigDecimal longitude,
	@NotBlank @Size(max = 50) String representativeName,
	@NotNull LocalDate foundedDate,
	@NotNull @PositiveOrZero Integer employeeCount,
	@Size(max = 255) String homepageUrl,
	@NotBlank @Size(max = 1500) String description,
	Long logoFileId,
	Long backgroundFileId,
	List<@NotNull CertificationType> certTypes,
	@Size(max = 20) List<@NotNull @Valid Equipment> equipments,
	@Size(max = 10) List<@NotNull Long> portfolioFileIds,
	@Size(max = 10) List<@NotNull Long> officeFileIds
) {

	/** 보유 설비 하나. 설비명과 사진(용도 COMPANY_EQUIPMENT)이 둘 다 필수다 */
	public record Equipment(
		@NotBlank @Size(max = 100) String name,
		@NotNull Long imageFileId
	) {
	}

	/** 목록을 보내지 않으면 빈 목록으로 본다 */
	public CompanyCreateRequest {
		certTypes = certTypes == null ? List.of() : certTypes;
		equipments = equipments == null ? List.of() : equipments;
		portfolioFileIds = portfolioFileIds == null ? List.of() : portfolioFileIds;
		officeFileIds = officeFileIds == null ? List.of() : officeFileIds;
	}

	public Company toEntity(Long memberId) {
		return Company.builder()
			.memberId(memberId)
			.name(name)
			.industry(industry)
			.products(products)
			.businessHours(businessHours)
			.contactPhone(contactPhone)
			.address(address)
			.addressDetail(addressDetail)
			.latitude(latitude)
			.longitude(longitude)
			.representativeName(representativeName)
			.foundedDate(foundedDate)
			.employeeCount(employeeCount)
			.homepageUrl(homepageUrl)
			.description(description)
			.logoFileId(logoFileId)
			.backgroundFileId(backgroundFileId)
			.build();
	}
}
