package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyVisibility;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	/** 삭제하지 않은 업체가 이미 있는지 (기업회원당 1개) */
	boolean existsByMemberIdAndDeletedAtIsNull(Long memberId);

	/** 회원이 등록한 업체 (삭제한 업체 제외). 노출 상태와 상관없이 찾는다 */
	Optional<Company> findByMemberIdAndDeletedAtIsNull(Long memberId);

	/** 삭제하지 않았고 노출 상태가 맞는 업체만 찾는다 */
	Optional<Company> findByCompanyIdAndVisibilityAndDeletedAtIsNull(Long companyId, CompanyVisibility visibility);
}
