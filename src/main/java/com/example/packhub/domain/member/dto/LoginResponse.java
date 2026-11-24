package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.entity.Role;

public record LoginResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long accessTokenExpiresIn,
	long refreshTokenExpiresIn,
	MemberSummary member
) {

	/**
	 * @param businessVerificationStatus 가장 최근 사업자 인증 상태(APPROVED 등). 신청한 적 없으면 NONE
	 */
	public record MemberSummary(
		Long memberId,
		MemberType memberType,
		Role role,
		String name,
		String businessVerificationStatus
	) {

		public static MemberSummary from(Member member, String businessVerificationStatus) {
			return new MemberSummary(member.getMemberId(), member.getMemberType(), member.getRole(), member.getName(),
				businessVerificationStatus);
		}
	}
}
