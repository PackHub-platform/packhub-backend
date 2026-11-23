package com.example.packhub.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

	// 회원·인증
	PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
	MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	MEMBER_PHONE_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호입니다."),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다. 고객센터에 문의해 주세요."),
	MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 로그인 정보입니다. 다시 로그인해 주세요."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인 기간이 만료되었습니다. 다시 로그인해 주세요."),
	REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해 주세요."),
	EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 완료해 주세요."),
	PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "휴대폰 인증을 완료해 주세요."),

	// 인증번호
	VERIFICATION_RESEND_TOO_EARLY(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요."),
	VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증번호를 먼저 요청해 주세요."),
	VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다."),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 다시 요청해 주세요."),
	VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증번호를 여러 번 틀렸습니다. 다시 요청해 주세요."),

	// 약관
	TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약관을 찾을 수 없습니다."),

	// 이용권·결제
	COMPANY_MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "기업회원만 이용할 수 있습니다."),
	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 플랜입니다."),
	CONSULT_ONLY_PLAN(HttpStatus.BAD_REQUEST, "상담 신청이 필요한 플랜입니다."),
	SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 이용 중인 이용권이 있습니다."),
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
	ORDER_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 주문입니다."),

	// 업체
	SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "이용권을 결제한 뒤 이용할 수 있습니다."),
	COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록한 업체가 있습니다."),
	INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "선택할 수 없는 카테고리입니다."),
	COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업체입니다."),
	COMPANY_REGISTRATION_REQUIRED(HttpStatus.FORBIDDEN, "업체를 등록한 뒤 이용할 수 있습니다."),

	// 사업자 인증
	INVALID_BUSINESS_NUMBER(HttpStatus.BAD_REQUEST, "사업자등록번호가 올바르지 않습니다."),
	BUSINESS_VERIFICATION_ALREADY_APPROVED(HttpStatus.CONFLICT, "이미 사업자 인증을 완료했습니다."),
	BUSINESS_VERIFICATION_IN_PROGRESS(HttpStatus.CONFLICT, "검토 중인 사업자 인증 신청이 있습니다."),
	BUSINESS_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "사업자 인증 신청 내역이 없습니다."),

	// 게시글
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

	// 파일
	INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
	FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 허용 범위를 벗어났습니다."),
	FILE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "업로드된 파일을 찾을 수 없습니다. 다시 업로드해 주세요."),
	FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "다른 회원의 파일입니다."),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다."),
	FILE_PURPOSE_MISMATCH(HttpStatus.BAD_REQUEST, "용도가 맞지 않는 파일입니다.");

	private final HttpStatus status;
	private final String message;
}
