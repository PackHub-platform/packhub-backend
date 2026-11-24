package com.example.packhub.domain.company.service;

import com.example.packhub.domain.category.entity.CategoryDomain;
import com.example.packhub.domain.category.service.CategoryService;
import com.example.packhub.domain.company.dto.CompanySaveRequest;
import com.example.packhub.domain.company.dto.CompanyCreateResponse;
import com.example.packhub.domain.company.dto.CompanyDetailResponse;
import com.example.packhub.domain.company.dto.CompanyFileResponse;
import com.example.packhub.domain.company.dto.CompanyPageResponse;
import com.example.packhub.domain.company.dto.CompanySearchRequest;
import com.example.packhub.domain.company.dto.CompanySummaryResponse;
import com.example.packhub.domain.company.entity.CertificationType;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyCategory;
import com.example.packhub.domain.company.entity.CompanyCertification;
import com.example.packhub.domain.company.entity.CompanyEquipment;
import com.example.packhub.domain.company.entity.CompanyImage;
import com.example.packhub.domain.company.entity.CompanyImageType;
import com.example.packhub.domain.company.entity.CompanyVisibility;
import com.example.packhub.domain.company.repository.CompanyCategoryRepository;
import com.example.packhub.domain.company.repository.CompanyCertificationRepository;
import com.example.packhub.domain.company.repository.CompanyEquipmentRepository;
import com.example.packhub.domain.company.repository.CompanyImageRepository;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.favorite.repository.FavoriteRepository;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.service.FileService;
import com.example.packhub.domain.file.service.FileUsageValidator;
import com.example.packhub.domain.member.service.CompanyMemberValidator;
import com.example.packhub.domain.subscription.service.SubscriptionService;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업체 등록·조회·수정·목록 조회. 등록은 이용권을 이용 중인 기업회원만 할 수 있고, 기업회원당 1개만 등록할 수 있다.
 * 조회수(Company.viewCount)는 쓰는 화면이 없어 기록하지 않는다 (2026-11-22 결정).
 * TODO(기말): 사업자 인증 완료 체크 추가 (관리자 검토가 생길 때 함께, 2026-11-24 결정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

	/** 기업회원당 업체 1개를 막는 DB 제약 이름 (V20261121_2330 마이그레이션) */
	private static final String ACTIVE_MEMBER_UNIQUE = "UK_COMPANY_ACTIVE_MEMBER";

	private final CompanyRepository companyRepository;
	private final CompanyCategoryRepository companyCategoryRepository;
	private final CompanyCertificationRepository companyCertificationRepository;
	private final CompanyEquipmentRepository companyEquipmentRepository;
	private final CompanyImageRepository companyImageRepository;
	private final CompanyMemberValidator companyMemberValidator;
	private final SubscriptionService subscriptionService;
	private final CategoryService categoryService;
	private final FileUsageValidator fileUsageValidator;
	private final FileService fileService;
	private final FavoriteRepository favoriteRepository;

	@Transactional
	public CompanyCreateResponse register(Long memberId, CompanySaveRequest request) {
		companyMemberValidator.validate(memberId);
		if (!subscriptionService.hasSubscriptionInUse(memberId)) {
			throw new BusinessException(ErrorCode.SUBSCRIPTION_REQUIRED);
		}
		if (companyRepository.existsByMemberIdAndDeletedAtIsNull(memberId)) {
			throw new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS);
		}
		categoryService.validateSelectable(request.categoryId(), CategoryDomain.COMMON);
		validateFiles(memberId, request);

		Long companyId = saveCompany(memberId, request);
		saveDetails(companyId, request);
		return new CompanyCreateResponse(companyId);
	}

	/**
	 * 노출 중인 업체 목록. 검색어·카테고리로 찾고 정렬해서 한 페이지만 돌려준다.
	 *
	 * @param memberId 로그인한 회원. 로그인하지 않았으면 null이고 favorited는 모두 false다
	 */
	public CompanyPageResponse search(CompanySearchRequest request, Long memberId) {
		boolean allCategories = request.categoryId() == null;
		List<Long> categoryIds = allCategories ? List.of()
			: categoryService.findFilterIds(request.categoryId(), CategoryDomain.COMMON);
		Page<Company> page = companyRepository.search(CompanyVisibility.VISIBLE, likePattern(request.keyword()),
			allCategories, categoryIds,
			PageRequest.of(request.page() - 1, request.size(), request.sort().getSort()));

		Map<Long, CompanyFileResponse> logos = toFileResponses(page.getContent().stream()
			.map(Company::getLogoFileId)
			.filter(Objects::nonNull)
			.distinct()
			.toList());
		Set<Long> favoriteCompanyIds = findFavoriteCompanyIds(memberId, page.getContent());
		return CompanyPageResponse.of(page, page.getContent().stream()
			.map(company -> CompanySummaryResponse.of(company, logos.get(company.getLogoFileId()),
				favoriteCompanyIds.contains(company.getCompanyId())))
			.toList());
	}

	/** 이 페이지의 업체 중 회원이 저장(관심)한 업체 */
	private Set<Long> findFavoriteCompanyIds(Long memberId, List<Company> companies) {
		if (memberId == null || companies.isEmpty()) {
			return Set.of();
		}
		return Set.copyOf(favoriteRepository.findCompanyIds(memberId,
			companies.stream().map(Company::getCompanyId).toList()));
	}

	/** 검색어의 %, _를 글자 그대로 찾도록 '!'로 이스케이프하고 앞뒤에 %를 붙인다. 검색어가 없으면 null */
	private String likePattern(String keyword) {
		if (keyword == null) {
			return null;
		}
		return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
	}

	/**
	 * 삭제했거나 숨김 상태인 업체는 404 COMPANY_NOT_FOUND.
	 *
	 * @param memberId 로그인한 회원. 로그인하지 않았으면 null이고 favorited는 false다
	 */
	public CompanyDetailResponse getDetail(Long companyId, Long memberId) {
		Company company = companyRepository
			.findByCompanyIdAndVisibilityAndDeletedAtIsNull(companyId, CompanyVisibility.VISIBLE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
		return toDetail(company, memberId);
	}

	/** 내 업체 조회. 숨김 상태여도 본인에게는 보인다 */
	public CompanyDetailResponse getMine(Long memberId) {
		companyMemberValidator.validate(memberId);
		return toDetail(findMine(memberId), memberId);
	}

	/**
	 * 받은 값으로 전체를 바꾼다. 목록은 기존 행을 지우고 다시 넣는다.
	 * 업체 행을 잠그고 시작한다. 잠그지 않으면 동시에 온 두 수정의 기본 정보와 목록이 섞이거나 목록이 두 번 들어간다.
	 */
	@Transactional
	public CompanyDetailResponse update(Long memberId, CompanySaveRequest request) {
		companyMemberValidator.validate(memberId);
		Company company = companyRepository.findForUpdateByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
		categoryService.validateSelectable(request.categoryId(), CategoryDomain.COMMON);
		validateFiles(memberId, request);

		request.applyTo(company);
		Long companyId = company.getCompanyId();
		companyCategoryRepository.deleteByCompanyId(companyId);
		companyCertificationRepository.deleteByCompanyId(companyId);
		companyEquipmentRepository.deleteByCompanyId(companyId);
		companyImageRepository.deleteByCompanyId(companyId);
		saveDetails(companyId, request);
		return toDetail(company, memberId);
	}

	private Company findMine(Long memberId) {
		return companyRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
	}

	private CompanyDetailResponse toDetail(Company company, Long memberId) {
		Long companyId = company.getCompanyId();
		List<CompanyCertification> certifications = companyCertificationRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
		List<CompanyEquipment> equipments = companyEquipmentRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
		List<CompanyImage> images = companyImageRepository.findByCompanyIdOrderBySortOrderAsc(companyId);

		boolean favorited = memberId != null && favoriteRepository.existsByMemberIdAndCompanyId(memberId, companyId);
		return CompanyDetailResponse.of(company, findCategory(companyId), certifications, equipments, images,
			findFiles(company, equipments, images), favorited);
	}

	/** 업체가 고른 소분류와 그 대분류 이름 */
	private CompanyDetailResponse.Category findCategory(Long companyId) {
		return companyCategoryRepository.findFirstByCompanyIdOrderByCompanyCategoryIdAsc(companyId)
			.flatMap(companyCategory -> categoryService.find(companyCategory.getCategoryId()))
			.map(category -> new CompanyDetailResponse.Category(category.getCategoryId(), category.getName(),
				category.getParentId(),
				categoryService.find(category.getParentId()).map(parent -> parent.getName()).orElse(null)))
			.orElse(null);
	}

	/** 로고·배경사진·설비 사진·포트폴리오·업체 사진의 fileId를 모아 한 번에 조회하고, 조회용 URL을 붙인다 */
	private Map<Long, CompanyFileResponse> findFiles(Company company, List<CompanyEquipment> equipments,
		List<CompanyImage> images) {
		List<Long> fileIds = Stream.of(
				Stream.of(company.getLogoFileId(), company.getBackgroundFileId()),
				equipments.stream().map(CompanyEquipment::getImageFileId),
				images.stream().map(CompanyImage::getFileId))
			.flatMap(ids -> ids)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		return toFileResponses(fileIds);
	}

	private Map<Long, CompanyFileResponse> toFileResponses(List<Long> fileIds) {
		return fileService.findFiles(fileIds).stream()
			.collect(Collectors.toMap(File::getFileId,
				file -> CompanyFileResponse.of(file, fileService.viewUrl(file.getStorageKey()))));
	}

	/** 동시에 들어온 등록 요청이 먼저 저장했으면 DB의 회원당 1개 제약에 걸린다. 그 경우만 409로 바꾼다 */
	private Long saveCompany(Long memberId, CompanySaveRequest request) {
		try {
			return companyRepository.saveAndFlush(request.toEntity(memberId)).getCompanyId();
		} catch (DataIntegrityViolationException e) {
			if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(ACTIVE_MEMBER_UNIQUE)) {
				throw new BusinessException(ErrorCode.COMPANY_ALREADY_EXISTS);
			}
			throw e;
		}
	}

	private void saveDetails(Long companyId, CompanySaveRequest request) {
		companyCategoryRepository.save(CompanyCategory.of(companyId, request.categoryId()));
		saveCertifications(companyId, request.certTypes());
		saveEquipments(companyId, request.equipments());
		saveImages(companyId, CompanyImageType.PORTFOLIO, request.portfolioFileIds());
		saveImages(companyId, CompanyImageType.OFFICE, request.officeFileIds());
	}

	/** 본인이 올린 파일인지, 자리에 맞는 용도로 올린 파일인지 확인한다 */
	private void validateFiles(Long memberId, CompanySaveRequest request) {
		fileUsageValidator.validate(memberId, FilePurpose.COMPANY_LOGO, singleOrEmpty(request.logoFileId()));
		fileUsageValidator.validate(memberId, FilePurpose.COMPANY_BACKGROUND, singleOrEmpty(request.backgroundFileId()));
		fileUsageValidator.validate(memberId, FilePurpose.COMPANY_EQUIPMENT, request.equipments().stream()
			.map(CompanySaveRequest.Equipment::imageFileId)
			.toList());
		fileUsageValidator.validate(memberId, FilePurpose.COMPANY_PORTFOLIO, request.portfolioFileIds());
		fileUsageValidator.validate(memberId, FilePurpose.COMPANY_IMAGE, request.officeFileIds());
	}

	private List<Long> singleOrEmpty(Long fileId) {
		return Stream.of(fileId).filter(Objects::nonNull).toList();
	}

	/** 같은 인증을 두 번 보내도 한 번만 저장한다 */
	private void saveCertifications(Long companyId, List<CertificationType> certTypes) {
		List<CertificationType> distinct = certTypes.stream().distinct().toList();
		companyCertificationRepository.saveAll(IntStream.range(0, distinct.size())
			.mapToObj(i -> CompanyCertification.of(companyId, distinct.get(i), i + 1))
			.toList());
	}

	private void saveEquipments(Long companyId, List<CompanySaveRequest.Equipment> equipments) {
		companyEquipmentRepository.saveAll(IntStream.range(0, equipments.size())
			.mapToObj(i -> CompanyEquipment.of(companyId, equipments.get(i).name(), equipments.get(i).imageFileId(), i + 1))
			.toList());
	}

	private void saveImages(Long companyId, CompanyImageType imageType, List<Long> fileIds) {
		companyImageRepository.saveAll(IntStream.range(0, fileIds.size())
			.mapToObj(i -> CompanyImage.of(companyId, imageType, fileIds.get(i), i + 1))
			.toList());
	}
}
