package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, Long> {

	/** 지금은 업체당 카테고리가 1개다 */
	Optional<CompanyCategory> findFirstByCompanyIdOrderByCompanyCategoryIdAsc(Long companyId);

	/** 수정할 때 기존 행을 지운다. 새 행을 넣기 전에 바로 실행되도록 쿼리로 지운다 */
	@Modifying
	@Query("DELETE FROM CompanyCategory c WHERE c.companyId = :companyId")
	void deleteByCompanyId(@Param("companyId") Long companyId);
}
