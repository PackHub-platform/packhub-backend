package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.CompanyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyCategoryRepository extends JpaRepository<CompanyCategory, Long> {
}
