package com.example.packhub.domain.member.controller;

import com.example.packhub.domain.member.dto.MyInfoResponse;
import com.example.packhub.domain.member.service.MemberService;
import com.example.packhub.global.common.ApiResponse;
import com.example.packhub.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "내 정보·권한 조회 (16-1)",
		description = "앱을 처음 열 때와 로그인 직후에 호출한다. memberType(기업회원인지), businessVerificationStatus(APPROVED인지), "
			+ "companyId(업체 등록했는지), hasActiveSubscription(이용권 이용 중인지)으로 화면 접근을 미리 판단한다. "
			+ "서버도 API마다 같은 조건을 다시 검사한다.")
	@GetMapping("/me")
	public ApiResponse<MyInfoResponse> getMe(@CurrentMember Long memberId) {
		return ApiResponse.ok(memberService.getMe(memberId));
	}
}
