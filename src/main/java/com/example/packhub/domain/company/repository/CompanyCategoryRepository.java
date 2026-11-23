package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, Long> {

	/** 지금은 업체당 카테고리가 1개다 */
	Optional<CompanyCategory> findFirstByCompanyIdOrderByCompanyCategoryIdAsc(Long companyId);
}
