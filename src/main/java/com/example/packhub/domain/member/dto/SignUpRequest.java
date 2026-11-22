package com.example.packhub.domain.member.dto;

import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.terms.dto.TermsAgreementRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 회원가입 요청. 인증번호 완료 여부 검사는 다음 이슈에서 추가한다.
 * agreements에는 GET /terms?usage=SIGNUP으로 받은 약관마다 동의 여부를 담는다.
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
	@Size(max = 50) String name,
	@NotNull @Valid List<@NotNull TermsAgreementRequest> agreements
) {
}
