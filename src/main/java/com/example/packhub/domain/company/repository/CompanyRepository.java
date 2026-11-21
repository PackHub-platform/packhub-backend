package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	/** 삭제하지 않은 업체가 이미 있는지 (기업회원당 1개) */
	boolean existsByMemberIdAndDeletedAtIsNull(Long memberId);
}
