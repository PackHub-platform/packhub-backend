package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyEquipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyEquipmentRepository extends JpaRepository<CompanyEquipment, Long> {

	List<CompanyEquipment> findByCompanyIdOrderBySortOrderAsc(Long companyId);

	/** 수정할 때 기존 행을 지운다. 새 행을 넣기 전에 바로 실행되도록 쿼리로 지운다 */
	@Modifying
	@Query("DELETE FROM CompanyEquipment c WHERE c.companyId = :companyId")
	void deleteByCompanyId(@Param("companyId") Long companyId);
}
