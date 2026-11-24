package com.example.packhub.domain.verification.service;

import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.verification.dto.VerificationConfirmResponse;
import com.example.packhub.domain.verification.dto.VerificationSendResponse;
import com.example.packhub.domain.verification.entity.VerificationChannel;
import com.example.packhub.domain.verification.entity.VerificationCode;
import com.example.packhub.domain.verification.repository.VerificationCodeRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 정보 입력(1-3)의 이메일·휴대폰 인증번호. 6자리, 5분 유효, 60초 뒤 재발송, 5회 틀리면 다시 발송,
 * 인증을 마치면 30분 안에 가입해야 한다. 저장소는 MySQL(VerificationCode)이다 (2026-11-24 결정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationService {

	private static final long EXPIRES_MINUTES = 5;
	private static final long RESEND_SECONDS = 60;
	private static final long VERIFIED_VALID_MINUTES = 30;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final VerificationCodeRepository verificationCodeRepository;
	private final MemberRepository memberRepository;
	private final VerificationCodeSender sender;

	/** 로컬·개발 서버에서만 true. 발송 응답에 인증번호를 같이 넣는다 */
	@Value("${packhub.verification.expose-code:false}")
	private boolean exposeCode;

	/**
	 * 인증번호를 새로 만들어 보낸다. 이미 가입된 이메일·번호면 409, 60초 안에 다시 요청하면 429
	 * VERIFICATION_RESEND_TOO_EARLY.
	 */
	@Transactional
	public VerificationSendResponse send(VerificationChannel channel, String target) {
		checkNotRegistered(channel, target);
		LocalDateTime now = LocalDateTime.now();
		findLatest(channel, target).ifPresent(latest -> {
			if (now.isBefore(latest.getCreatedAt().plusSeconds(RESEND_SECONDS))) {
				throw new BusinessException(ErrorCode.VERIFICATION_RESEND_TOO_EARLY);
			}
		});

		String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
		VerificationCode saved = verificationCodeRepository.save(
			VerificationCode.issue(channel, target, hash(code), now.plusMinutes(EXPIRES_MINUTES)));
		sender.send(channel, target, code);
		return new VerificationSendResponse(target, saved.getExpiresAt(), now.plusSeconds(RESEND_SECONDS),
			exposeCode ? code : null);
	}

	/**
	 * 가장 최근에 보낸 인증번호와 맞춰 본다. 틀린 횟수는 에러를 돌려줘도 저장해야 해서 BusinessException에 되돌리지 않는다.
	 * 발송 기록이 없으면 404, 5회 틀렸으면 429, 5분이 지났으면 400 EXPIRED, 틀리면 400 MISMATCH.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public VerificationConfirmResponse confirm(VerificationChannel channel, String target, String code) {
		VerificationCode latest = findLatest(channel, target)
			.orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));
		LocalDateTime now = LocalDateTime.now();
		if (latest.isVerified()) {
			return new VerificationConfirmResponse(target, true, latest.getVerifiedAt());
		}
		if (latest.attemptsExceeded()) {
			throw new BusinessException(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
		}
		if (latest.isExpired(now)) {
			throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}
		if (!latest.matches(hash(code))) {
			latest.recordFailure();
			throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
		}
		latest.markVerified(now);
		return new VerificationConfirmResponse(target, true, now);
	}

	/** 가입할 때: 이 이메일·번호가 30분 안에 인증을 마쳤는지. 아니면 400 EMAIL_NOT_VERIFIED / PHONE_NOT_VERIFIED */
	public void checkVerified(VerificationChannel channel, String target) {
		boolean verified = findLatest(channel, target)
			.map(latest -> latest.isVerifiedWithin(LocalDateTime.now(), VERIFIED_VALID_MINUTES))
			.orElse(false);
		if (!verified) {
			throw new BusinessException(channel == VerificationChannel.EMAIL
				? ErrorCode.EMAIL_NOT_VERIFIED : ErrorCode.PHONE_NOT_VERIFIED);
		}
	}

	private void checkNotRegistered(VerificationChannel channel, String target) {
		if (channel == VerificationChannel.EMAIL && memberRepository.existsByEmail(target)) {
			throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
		}
		if (channel == VerificationChannel.PHONE && memberRepository.existsByPhone(target)) {
			throw new BusinessException(ErrorCode.MEMBER_PHONE_DUPLICATED);
		}
	}

	private java.util.Optional<VerificationCode> findLatest(VerificationChannel channel, String target) {
		return verificationCodeRepository.findFirstByChannelAndTargetOrderByVerificationCodeIdDesc(channel, target);
	}

	private static String hash(String code) {
		try {
			return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
