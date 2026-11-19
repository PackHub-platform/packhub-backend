package com.example.packhub.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

	// 이용권·결제
	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 플랜입니다."),
	CONSULT_ONLY_PLAN(HttpStatus.BAD_REQUEST, "상담 신청이 필요한 플랜입니다."),
	SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 이용 중인 이용권이 있습니다."),
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
	ORDER_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 주문입니다.");

	private final HttpStatus status;
	private final String message;
}
