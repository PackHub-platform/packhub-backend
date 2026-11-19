package com.example.packhub.domain.subscription.repository;

import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.entity.SubscriptionStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findFirstByMemberIdAndStatusIn(Long memberId, Collection<SubscriptionStatus> statuses);

	boolean existsByMemberIdAndStatusIn(Long memberId, Collection<SubscriptionStatus> statuses);
}
