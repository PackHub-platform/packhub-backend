package com.example.packhub.domain.business.service;

import java.util.regex.Pattern;

/**
 * 사업자등록번호 형식·검증 숫자 확인. 진짜 있는 번호인지(국세청 조회)는 기말 범위다.
 * 검증 숫자: 앞 9자리에 1,3,7,1,3,7,1,3,5를 곱해 더하고, 9번째 자리 × 5의 십의 자리를 더한 합으로 마지막 자리를 계산한다.
 */
final class BusinessNumber {

	private static final Pattern FORMAT = Pattern.compile("\\d{3}-\\d{2}-\\d{5}");
	private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

	private BusinessNumber() {
	}

	static boolean isValid(String value) {
		if (value == null || !FORMAT.matcher(value).matches()) {
			return false;
		}
		String digits = value.replace("-", "");
		int sum = 0;
		for (int i = 0; i < WEIGHTS.length; i++) {
			sum += (digits.charAt(i) - '0') * WEIGHTS[i];
		}
		sum += (digits.charAt(8) - '0') * 5 / 10;
		int check = (10 - sum % 10) % 10;
		return check == digits.charAt(9) - '0';
	}
}
