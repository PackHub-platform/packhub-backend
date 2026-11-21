package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {

	List<CompanyImage> findByCompanyIdOrderBySortOrderAsc(Long companyId);
}
