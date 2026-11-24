package com.example.packhub.domain.verification.repository;

import com.example.packhub.domain.verification.entity.VerificationChannel;
import com.example.packhub.domain.verification.entity.VerificationCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

	/** 이 수단·대상으로 가장 최근에 보낸 인증번호 */
	Optional<VerificationCode> findFirstByChannelAndTargetOrderByVerificationCodeIdDesc(VerificationChannel channel,
		String target);

	/**
	 * 확인 요청용. 같은 대상의 확인 요청이 동시에 와도 틀린 횟수가 빠짐없이 쌓이도록 행을 잠그고 조회한다
	 * (잠그지 않으면 두 요청이 같은 횟수를 읽고 둘 다 +1만 저장해서 5회 제한을 넘겨 시도할 수 있다)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT v FROM VerificationCode v
		WHERE v.channel = :channel AND v.target = :target
		ORDER BY v.verificationCodeId DESC
		LIMIT 1
		""")
	Optional<VerificationCode> findLatestForUpdate(@Param("channel") VerificationChannel channel,
		@Param("target") String target);

	/** 가입 검사용. 인증을 마친 가장 최근 기록 (인증 뒤에 다시 발송해도 이 기록으로 판단한다) */
	Optional<VerificationCode> findFirstByChannelAndTargetAndVerifiedAtIsNotNullOrderByVerificationCodeIdDesc(
		VerificationChannel channel, String target);
}
