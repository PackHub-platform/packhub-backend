package com.example.packhub.domain.member.entity;

public enum MemberStatus {
	/** 정상 */
	ACTIVE,
	/** 관리자가 정지한 계정. 로그인할 수 없다 */
	SUSPENDED,
	/** 회원탈퇴. 이메일·휴대폰은 마스킹된다 */
	WITHDRAWN
}
