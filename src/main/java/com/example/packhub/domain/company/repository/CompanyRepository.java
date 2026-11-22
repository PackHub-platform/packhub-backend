package com.example.packhub.domain.company.repository;

import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyVisibility;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	/** 삭제하지 않은 업체가 이미 있는지 (기업회원당 1개) */
	boolean existsByMemberIdAndDeletedAtIsNull(Long memberId);

	/** 삭제하지 않았고 노출 상태가 맞는 업체가 있는지 */
	boolean existsByCompanyIdAndVisibilityAndDeletedAtIsNull(Long companyId, CompanyVisibility visibility);

	/** 회원이 등록한 업체 (삭제한 업체 제외). 노출 상태와 상관없이 찾는다 */
	Optional<Company> findByMemberIdAndDeletedAtIsNull(Long memberId);

	/** 삭제하지 않았고 노출 상태가 맞는 업체만 찾는다 */
	Optional<Company> findByCompanyIdAndVisibilityAndDeletedAtIsNull(Long companyId, CompanyVisibility visibility);

	/**
	 * 업체 목록. 삭제하지 않았고 노출 상태가 맞는 업체 중에서 찾는다.
	 *
	 * @param keyword       업체명·업종·생산품목에 쓸 LIKE 패턴('!'로 이스케이프). null이면 검색어 조건 없음
	 * @param allCategories true면 카테고리 조건 없음
	 * @param categoryIds   업체의 카테고리(소분류)가 이 중 하나여야 한다
	 */
	@Query("""
		SELECT c FROM Company c
		WHERE c.deletedAt IS NULL AND c.visibility = :visibility
		  AND (:keyword IS NULL OR c.name LIKE :keyword ESCAPE '!'
		       OR c.industry LIKE :keyword ESCAPE '!' OR c.products LIKE :keyword ESCAPE '!')
		  AND (:allCategories = TRUE OR EXISTS (
		       SELECT 1 FROM CompanyCategory cc
		       WHERE cc.companyId = c.companyId AND cc.categoryId IN :categoryIds))
		""")
	Page<Company> search(@Param("visibility") CompanyVisibility visibility, @Param("keyword") String keyword,
		@Param("allCategories") boolean allCategories, @Param("categoryIds") Collection<Long> categoryIds,
		Pageable pageable);
}
