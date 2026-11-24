package com.example.packhub.domain.business.service;

import com.example.packhub.domain.business.dto.BusinessVerificationRequest;
import com.example.packhub.domain.business.dto.BusinessVerificationResponse;
import com.example.packhub.domain.business.entity.BusinessVerification;
import com.example.packhub.domain.business.entity.BusinessVerificationStatus;
import com.example.packhub.domain.business.repository.BusinessVerificationRepository;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.service.FileUsageValidator;
import com.example.packhub.domain.member.service.CompanyMemberValidator;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사업자 인증 (가입 1-4). 기업회원만 쓸 수 있다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessVerificationService {

	private final BusinessVerificationRepository businessVerificationRepository;
	private final CompanyMemberValidator companyMemberValidator;
	private final FileUsageValidator fileUsageValidator;

	/**
	 * 신청하면 바로 승인한다 (중간 범위). 이미 승인됐으면 409 BUSINESS_VERIFICATION_ALREADY_APPROVED,
	 * 검토 중인 신청이 있으면 409 BUSINESS_VERIFICATION_IN_PROGRESS, 사업자번호가 틀리면 400 INVALID_BUSINESS_NUMBER.
	 */
	@Transactional
	public BusinessVerificationResponse apply(Long memberId, BusinessVerificationRequest request) {
		companyMemberValidator.validate(memberId);
		findLatest(memberId).ifPresent(latest -> {
			if (latest.getStatus() == BusinessVerificationStatus.APPROVED) {
				throw new BusinessException(ErrorCode.BUSINESS_VERIFICATION_ALREADY_APPROVED);
			}
			if (latest.getStatus() == BusinessVerificationStatus.PENDING) {
				throw new BusinessException(ErrorCode.BUSINESS_VERIFICATION_IN_PROGRESS);
			}
		});
		if (!BusinessNumber.isValid(request.businessNumber())) {
			throw new BusinessException(ErrorCode.INVALID_BUSINESS_NUMBER);
		}
		fileUsageValidator.validate(memberId, FilePurpose.BUSINESS_CERTIFICATE, List.of(request.certificateFileId()));

		return BusinessVerificationResponse.from(businessVerificationRepository.save(
			BusinessVerification.approveNow(memberId, request.businessName(), request.representativeName(),
				request.businessNumber(), request.certificateFileId(), LocalDateTime.now())));
	}

	/** 가장 최근 신청. 신청한 적 없으면 404 BUSINESS_VERIFICATION_NOT_FOUND */
	public BusinessVerificationResponse getMine(Long memberId) {
		companyMemberValidator.validate(memberId);
		return findLatest(memberId).map(BusinessVerificationResponse::from)
			.orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_VERIFICATION_NOT_FOUND));
	}

	/** 로그인 응답·내 정보용 상태 이름. 신청한 적 없으면 NONE */
	public String statusOf(Long memberId) {
		return findLatest(memberId).map(latest -> latest.getStatus().name()).orElse("NONE");
	}

	/** 사업자 인증을 마쳐야 쓸 수 있는 기능(업체 등록, 결제)에서 호출한다. 아니면 403 BUSINESS_NOT_VERIFIED */
	public void validateApproved(Long memberId) {
		boolean approved = findLatest(memberId)
			.map(latest -> latest.getStatus() == BusinessVerificationStatus.APPROVED)
			.orElse(false);
		if (!approved) {
			throw new BusinessException(ErrorCode.BUSINESS_NOT_VERIFIED);
		}
	}

	private Optional<BusinessVerification> findLatest(Long memberId) {
		return businessVerificationRepository.findFirstByMemberIdOrderByBusinessVerificationIdDesc(memberId);
	}
}
