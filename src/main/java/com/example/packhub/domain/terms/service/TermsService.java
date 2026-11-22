package com.example.packhub.domain.terms.service;

import com.example.packhub.domain.terms.dto.TermsAgreementRequest;
import com.example.packhub.domain.terms.dto.TermsDetailResponse;
import com.example.packhub.domain.terms.dto.TermsSummaryResponse;
import com.example.packhub.domain.terms.entity.MemberTermsAgreement;
import com.example.packhub.domain.terms.entity.Terms;
import com.example.packhub.domain.terms.entity.TermsType;
import com.example.packhub.domain.terms.entity.TermsUsage;
import com.example.packhub.domain.terms.repository.MemberTermsAgreementRepository;
import com.example.packhub.domain.terms.repository.TermsRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

	private final TermsRepository termsRepository;
	private final MemberTermsAgreementRepository agreementRepository;

	/** 용도에 해당하는 약관을 종류마다 시행 중인 최신 버전 하나씩, 화면 순서대로 준다 */
	public List<TermsSummaryResponse> getTermsList(TermsUsage usage) {
		return findCurrentTerms(usage).stream()
			.map(TermsSummaryResponse::from)
			.toList();
	}

	/** version이 없으면 시행 중인 최신 버전을 준다 */
	public TermsDetailResponse getTerms(TermsType termsType, String version) {
		Terms terms = (version == null || version.isBlank())
			? findCurrentTerms(termsType)
			: termsRepository.findByTermsTypeAndVersion(termsType, version)
				.orElseThrow(() -> new BusinessException(ErrorCode.TERMS_NOT_FOUND));
		return TermsDetailResponse.from(terms);
	}

	/**
	 * 가입 약관 동의 내역을 검사한다. 회원을 만들기 전에 부른다.
	 * 지금 시행 중인 가입 약관의 termsId만 받는다. 옛 버전·없는 ID·중복이 섞여 있거나
	 * 필수 약관 하나라도 동의하지 않았으면 REQUIRED_TERMS_NOT_AGREED.
	 */
	public void checkSignUpAgreements(List<TermsAgreementRequest> agreements) {
		Map<Long, Terms> currentById = new HashMap<>();
		findCurrentTerms(TermsUsage.SIGNUP).forEach(terms -> currentById.put(terms.getTermsId(), terms));

		Map<Long, Boolean> agreedById = new HashMap<>();
		for (TermsAgreementRequest agreement : agreements) {
			boolean isCurrent = currentById.containsKey(agreement.termsId());
			boolean isDuplicated = agreedById.put(agreement.termsId(), agreement.agreed()) != null;
			if (!isCurrent || isDuplicated) {
				throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
			}
		}

		boolean allRequiredAgreed = currentById.values().stream()
			.filter(Terms::isRequired)
			.allMatch(terms -> Boolean.TRUE.equals(agreedById.get(terms.getTermsId())));
		if (!allRequiredAgreed) {
			throw new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
		}
	}

	/** checkSignUpAgreements를 통과한 동의 내역을 저장한다. 선택 약관 거부도 agreed=false로 남긴다 */
	@Transactional
	public void saveAgreements(Long memberId, List<TermsAgreementRequest> agreements, LocalDateTime agreedAt) {
		agreementRepository.saveAll(agreements.stream()
			.map(agreement -> MemberTermsAgreement.of(memberId, agreement.termsId(), agreement.agreed(), agreedAt))
			.toList());
	}

	private List<Terms> findCurrentTerms(TermsUsage usage) {
		Map<TermsType, Terms> latestByType = findLatestByType(usage.getTermsTypes());
		return usage.getTermsTypes().stream()
			.map(latestByType::get)
			.filter(Objects::nonNull)
			.toList();
	}

	private Terms findCurrentTerms(TermsType termsType) {
		Terms terms = findLatestByType(List.of(termsType)).get(termsType);
		if (terms == null) {
			throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
		}
		return terms;
	}

	/** 시행일 최신순으로 받아서, 종류마다 처음 나온 것(=최신)만 남긴다 */
	private Map<TermsType, Terms> findLatestByType(List<TermsType> termsTypes) {
		Map<TermsType, Terms> latestByType = new EnumMap<>(TermsType.class);
		termsRepository.findByTermsTypeInAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(termsTypes,
				LocalDateTime.now())
			.forEach(terms -> latestByType.putIfAbsent(terms.getTermsType(), terms));
		return latestByType;
	}
}
