package com.example.packhub.domain.verification.repository;

import com.example.packhub.domain.verification.entity.VerificationChannel;
import com.example.packhub.domain.verification.entity.VerificationCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

	/** 이 수단·대상으로 가장 최근에 보낸 인증번호 */
	Optional<VerificationCode> findFirstByChannelAndTargetOrderByVerificationCodeIdDesc(VerificationChannel channel,
		String target);
}
