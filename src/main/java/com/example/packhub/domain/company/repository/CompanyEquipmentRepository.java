package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyEquipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyEquipmentRepository extends JpaRepository<CompanyEquipment, Long> {

	List<CompanyEquipment> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
