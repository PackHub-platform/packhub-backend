package com.example.packhub.domain.subscription.repository;

import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.entity.SubscriptionStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	/** 상태가 맞고 이용 기간이 아직 끝나지 않은(종료일이 today 이후인) 이용권 */
	Optional<Subscription> findFirstByMemberIdAndStatusInAndCurrentPeriodEndGreaterThanEqual(Long memberId,
		Collection<SubscriptionStatus> statuses, LocalDate today);

	boolean existsByMemberIdAndStatusInAndCurrentPeriodEndGreaterThanEqual(Long memberId,
		Collection<SubscriptionStatus> statuses, LocalDate today);
}
