package com.example.packhub.domain.subscription.repository;

import com.example.packhub.domain.subscription.entity.Payment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/** 오늘 마지막 주문번호를 찾는다 (prefix 예: "PH-20261119-"). */
	Optional<Payment> findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix);

	/** 같은 주문이 동시에 두 번 승인되지 않도록 행을 잠그고 조회한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Payment> findForUpdateByOrderNo(String orderNo);

	List<Payment> findTop5ByMemberIdOrderByPaymentIdDesc(Long memberId);
}
