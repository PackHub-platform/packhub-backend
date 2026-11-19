package com.example.packhub.domain.member.controller;

import com.example.packhub.domain.member.dto.LoginRequest;
import com.example.packhub.domain.member.dto.LoginResponse;
import com.example.packhub.domain.member.dto.SignUpRequest;
import com.example.packhub.domain.member.dto.SignUpResponse;
import com.example.packhub.domain.member.service.AuthService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /api/v1/auth/** 는 SecurityConfig에서 로그인 없이 열려 있다 */
@Tag(name = "인증")
@SecurityRequirements // Swagger에서 이 API들은 토큰 없이 호출한다고 표시
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "회원가입 (1-1, 1-2, 1-3, 1-5)",
		description = "가입하고 바로 Access Token을 준다. 기업회원이면 nextStep이 BUSINESS_VERIFICATION이다. 인증번호·약관 검사는 아직 없다.")
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ApiResponse.ok(authService.signUp(request));
	}

	@Operation(summary = "로그인 (2-1)",
		description = "이메일이 없거나 비밀번호가 틀리면 같은 LOGIN_FAILED를 준다. 받은 accessToken을 Swagger 오른쪽 위 Authorize에 넣으면 로그인이 필요한 API를 호출할 수 있다.")
	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}
}
