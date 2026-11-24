package com.example.packhub.domain.verification.service;

import com.example.packhub.domain.verification.entity.VerificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 인증번호를 실제로 보내는 곳. 문자 업체·메일 서버가 정해지기 전까지 서버 로그로 출력한다 (2026-11-24 결정).
 * 나중에 문자·메일 발송을 붙이면 이 클래스만 바꾸면 된다.
 */
@Slf4j
@Component
public class VerificationCodeSender {

	public void send(VerificationChannel channel, String target, String code) {
		log.info("[인증번호] {} {} → {}", channel, target, code);
	}
}
