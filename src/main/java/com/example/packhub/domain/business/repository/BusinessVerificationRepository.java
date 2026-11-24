package com.example.packhub.domain.business.repository;

import com.example.packhub.domain.business.entity.BusinessVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessVerificationRepository extends JpaRepository<BusinessVerification, Long> {

	/** 회원의 가장 최근 신청 (지금 상태) */
	Optional<BusinessVerification> findFirstByMemberIdOrderByBusinessVerificationIdDesc(Long memberId);
}
