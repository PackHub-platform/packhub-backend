package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyCertification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyCertificationRepository extends JpaRepository<CompanyCertification, Long> {

	List<CompanyCertification> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
