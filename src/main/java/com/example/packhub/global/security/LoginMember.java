package com.example.packhub.global.security;

/**
 * 토큰 검증에 성공한 요청의 회원 정보. SecurityContext에 로그인 표시로 들어간다.
 *
 * @param memberId 회원 ID (Member.memberId)
 * @param role     권한 (USER / ADMIN)
 */
public record LoginMember(Long memberId, String role) {
}
