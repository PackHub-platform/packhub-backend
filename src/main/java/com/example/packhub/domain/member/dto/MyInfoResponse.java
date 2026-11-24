package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberStatus;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.entity.Role;

/**
 * 내 정보·권한 (16-1 접근 제어). 프론트가 화면에 들어갈 수 있는지 미리 판단할 때 쓴다.
 *
 * @param businessVerificationStatus 가장 최근 사업자 인증 상태. 신청한 적 없으면 NONE
 * @param companyId                  등록한 업체. 없으면 null
 * @param hasActiveSubscription      이용 중인 이용권이 있는지
 */
public record MyInfoResponse(
	Long memberId,
	String email,
	String name,
	String phone,
	MemberType memberType,
	Role role,
	MemberStatus status,
	String businessVerificationStatus,
	Long companyId,
	boolean hasActiveSubscription
) {

	public static MyInfoResponse of(Member member, String businessVerificationStatus, Long companyId,
		boolean hasActiveSubscription) {
		return new MyInfoResponse(member.getMemberId(), member.getEmail(), member.getName(), member.getPhone(),
			member.getMemberType(), member.getRole(), member.getStatus(), businessVerificationStatus, companyId,
			hasActiveSubscription);
	}
}
