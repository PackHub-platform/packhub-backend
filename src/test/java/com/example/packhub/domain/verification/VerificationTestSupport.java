package com.example.packhub.domain.verification;

import com.example.packhub.domain.verification.entity.VerificationChannel;
import com.example.packhub.domain.verification.entity.VerificationCode;
import com.example.packhub.domain.verification.repository.VerificationCodeRepository;
import java.time.LocalDateTime;

/** 가입 테스트용: 이메일·휴대폰 인증을 방금 마친 상태를 DB에 바로 만든다 (가입은 30분 안의 인증을 검사한다) */
public final class VerificationTestSupport {

	private VerificationTestSupport() {
	}

	public static void markVerified(VerificationCodeRepository repository, String email, String phone) {
		save(repository, VerificationChannel.EMAIL, email);
		save(repository, VerificationChannel.PHONE, phone);
	}

	private static void save(VerificationCodeRepository repository, VerificationChannel channel, String target) {
		LocalDateTime now = LocalDateTime.now();
		VerificationCode code = VerificationCode.issue(channel, target, "test", now.plusMinutes(5));
		code.markVerified(now);
		repository.save(code);
	}
}
