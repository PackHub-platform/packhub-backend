package com.example.packhub.domain.subscription.repository;

import com.example.packhub.domain.subscription.entity.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

	List<Plan> findByIsActiveTrueOrderByPlanIdAsc();
}
