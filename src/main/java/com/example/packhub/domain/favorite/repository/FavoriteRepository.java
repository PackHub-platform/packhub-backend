package com.example.packhub.domain.favorite.repository;

import com.example.packhub.domain.company.entity.CompanyVisibility;
import com.example.packhub.domain.favorite.dto.FavoriteResponse;
import com.example.packhub.domain.favorite.entity.Favorite;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	boolean existsByMemberIdAndCompanyId(Long memberId, Long companyId);

	@Modifying
	@Query("DELETE FROM Favorite f WHERE f.memberId = :memberId AND f.companyId = :companyId")
	void deleteCompany(@Param("memberId") Long memberId, @Param("companyId") Long companyId);

	/** 주어진 업체들 중 회원이 저장한 업체의 ID (업체 목록의 favorited 표시용) */
	@Query("SELECT f.companyId FROM Favorite f WHERE f.memberId = :memberId AND f.companyId IN :companyIds")
	List<Long> findCompanyIds(@Param("memberId") Long memberId, @Param("companyIds") Collection<Long> companyIds);

	/** 저장한 업체 목록. 삭제했거나 노출 상태가 맞지 않는 업체는 빠진다. 최근에 저장한 순서다 */
	@Query("""
		SELECT new com.example.packhub.domain.favorite.dto.FavoriteResponse(
		       f.targetType, c.companyId, c.name, c.description, f.createdAt)
		FROM Favorite f JOIN Company c ON c.companyId = f.companyId
		WHERE f.memberId = :memberId AND c.deletedAt IS NULL AND c.visibility = :visibility
		ORDER BY f.createdAt DESC, f.favoriteId DESC
		""")
	Page<FavoriteResponse> findCompanies(@Param("memberId") Long memberId,
		@Param("visibility") CompanyVisibility visibility, Pageable pageable);
}
