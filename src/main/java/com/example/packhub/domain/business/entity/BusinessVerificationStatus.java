package com.example.packhub.domain.business.entity;

/** 사업자 인증 상태. 중간 범위에서는 신청하면 바로 APPROVED다 (관리자 검토가 생기면 PENDING부터) */
public enum BusinessVerificationStatus {
	PENDING,
	APPROVED,
	REJECTED
}
