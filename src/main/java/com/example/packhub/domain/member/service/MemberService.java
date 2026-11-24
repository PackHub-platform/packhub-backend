package com.example.packhub.domain.member.service;

import com.example.packhub.domain.business.service.BusinessVerificationService;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.member.dto.MyInfoResponse;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.subscription.service.SubscriptionService;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인한 회원 자신의 정보 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final CompanyRepository companyRepository;
	private final SubscriptionService subscriptionService;
	private final BusinessVerificationService businessVerificationService;

	/** 내 정보·권한 (16-1). 회원·사업자 인증·업체·이용권 상태를 한 번에 돌려준다 */
	public MyInfoResponse getMe(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		Long companyId = companyRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.map(Company::getCompanyId)
			.orElse(null);
		return MyInfoResponse.of(member, businessVerificationService.statusOf(memberId), companyId,
			subscriptionService.hasSubscriptionInUse(memberId));
	}
}
