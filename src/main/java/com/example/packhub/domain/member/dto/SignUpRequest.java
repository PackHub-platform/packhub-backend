package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청. 인증번호 완료 여부와 약관 동의(agreements)는 다음 이슈에서 추가한다.
 */
public record SignUpRequest(
	@NotNull MemberType memberType,
	@NotBlank @Email @Size(max = 255) String email,
	@NotBlank
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,20}$",
		message = "비밀번호는 영문·숫자·특수문자를 포함해 8~20자여야 합니다.")
	String password,
	@NotBlank String passwordConfirm,
	@NotBlank @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력해 주세요.") String phone,
	@Size(max = 50) String name
) {
}
