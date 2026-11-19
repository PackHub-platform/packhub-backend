package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.entity.Role;

/**
 * refreshToken·refreshTokenExpiresIn은 재발급·로그아웃 이슈에서 추가한다.
 */
public record LoginResponse(
	String accessToken,
	String tokenType,
	long accessTokenExpiresIn,
	MemberSummary member
) {

	/**
	 * @param businessVerificationStatus 사업자 인증 기능 전까지 NONE으로 고정
	 */
	public record MemberSummary(
		Long memberId,
		MemberType memberType,
		Role role,
		String name,
		String businessVerificationStatus
	) {

		public static MemberSummary from(Member member) {
			return new MemberSummary(member.getMemberId(), member.getMemberType(), member.getRole(), member.getName(),
				"NONE");
		}
	}
}
