package com.example.packhub.domain.member.service;

import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 기업회원만 쓸 수 있는 기능(이용권 결제, 파트너 상담, 업체 등록 등)에서 호출한다.
 * 토큰에는 회원 유형이 없어서 회원을 조회해 확인한다.
 */
@Component
@RequiredArgsConstructor
public class CompanyMemberValidator {

	private final MemberRepository memberRepository;

	public void validate(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (member.getMemberType() != MemberType.COMPANY) {
			throw new BusinessException(ErrorCode.COMPANY_MEMBER_REQUIRED);
		}
	}
}
