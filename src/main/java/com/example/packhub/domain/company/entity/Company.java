package com.example.packhub.domain.company.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업회원이 등록한 업체 (기업회원당 1개).
 * 카테고리·인증·설비·사진은 Company의 컬럼이 아니라 각자 테이블(CompanyCategory 등)에 companyId로 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long companyId;

	@Column(nullable = false)
	private Long memberId;

	@Column(nullable = false, length = 100)
	private String name;

	/** 업종 (직접 입력) */
	@Column(nullable = false, length = 50)
	private String industry;

	/** 생산품목 (직접 입력). 카테고리와 다른 값이다 */
	@Column(nullable = false, length = 255)
	private String products;

	@Column(nullable = false, length = 50)
	private String businessHours;

	@Column(nullable = false, length = 20)
	private String contactPhone;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(length = 255)
	private String addressDetail;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(nullable = false, length = 50)
	private String representativeName;

	@Column(nullable = false)
	private LocalDate foundedDate;

	@Column(nullable = false)
	private Integer employeeCount;

	@Column(length = 255)
	private String homepageUrl;

	@Column(nullable = false, length = 1500)
	private String description;

	private Long logoFileId;

	private Long backgroundFileId;

	@Column(nullable = false)
	private int viewCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CompanyVisibility visibility;

	private LocalDateTime deletedAt;

	/** 등록. 조회수 0, 노출(VISIBLE) 상태로 시작한다 */
	@Builder
	private Company(Long memberId, String name, String industry, String products, String businessHours,
		String contactPhone, String address, String addressDetail, BigDecimal latitude, BigDecimal longitude,
		String representativeName, LocalDate foundedDate, Integer employeeCount, String homepageUrl,
		String description, Long logoFileId, Long backgroundFileId) {
		this.memberId = memberId;
		this.name = name;
		this.industry = industry;
		this.products = products;
		this.businessHours = businessHours;
		this.contactPhone = contactPhone;
		this.address = address;
		this.addressDetail = addressDetail;
		this.latitude = latitude;
		this.longitude = longitude;
		this.representativeName = representativeName;
		this.foundedDate = foundedDate;
		this.employeeCount = employeeCount;
		this.homepageUrl = homepageUrl;
		this.description = description;
		this.logoFileId = logoFileId;
		this.backgroundFileId = backgroundFileId;
		this.viewCount = 0;
		this.visibility = CompanyVisibility.VISIBLE;
	}

	/** 정보 수정. 소유 회원·조회수·노출 상태는 바꾸지 않는다 */
	public void update(String name, String industry, String products, String businessHours,
		String contactPhone, String address, String addressDetail, BigDecimal latitude, BigDecimal longitude,
		String representativeName, LocalDate foundedDate, Integer employeeCount, String homepageUrl,
		String description, Long logoFileId, Long backgroundFileId) {
		this.name = name;
		this.industry = industry;
		this.products = products;
		this.businessHours = businessHours;
		this.contactPhone = contactPhone;
		this.address = address;
		this.addressDetail = addressDetail;
		this.latitude = latitude;
		this.longitude = longitude;
		this.representativeName = representativeName;
		this.foundedDate = foundedDate;
		this.employeeCount = employeeCount;
		this.homepageUrl = homepageUrl;
		this.description = description;
		this.logoFileId = logoFileId;
		this.backgroundFileId = backgroundFileId;
	}
}
