package com.example.packhub.domain.favorite.repository;

import com.example.packhub.domain.company.entity.CompanyVisibility;
import com.example.packhub.domain.favorite.dto.FavoriteResponse;
import com.example.packhub.domain.favorite.entity.Favorite;
import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import com.example.packhub.domain.post.entity.PostVisibility;
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

	boolean existsByMemberIdAndPostId(Long memberId, Long postId);

	@Modifying
	@Query("DELETE FROM Favorite f WHERE f.memberId = :memberId AND f.postId = :postId")
	void deletePost(@Param("memberId") Long memberId, @Param("postId") Long postId);

	/** 주어진 게시글들 중 회원이 저장한 게시글의 ID (의뢰 목록의 favorited 표시용) */
	@Query("SELECT f.postId FROM Favorite f WHERE f.memberId = :memberId AND f.postId IN :postIds")
	List<Long> findPostIds(@Param("memberId") Long memberId, @Param("postIds") Collection<Long> postIds);

	/**
	 * 저장한 업체·게시글 카드. 삭제했거나 노출 상태가 맞지 않는 대상은 빠진다. 최근에 저장한 순서다.
	 *
	 * @param targetType null이면 업체와 게시글 모두
	 */
	@Query(value = """
		SELECT new com.example.packhub.domain.favorite.dto.FavoriteResponse(
		       f.targetType, COALESCE(c.companyId, p.postId), COALESCE(c.name, p.title),
		       COALESCE(c.description, p.content), f.createdAt)
		FROM Favorite f
		LEFT JOIN Company c ON c.companyId = f.companyId AND c.deletedAt IS NULL AND c.visibility = :companyVisibility
		LEFT JOIN Post p ON p.postId = f.postId AND p.deletedAt IS NULL AND p.visibility = :postVisibility
		WHERE f.memberId = :memberId
		  AND (:targetType IS NULL OR f.targetType = :targetType)
		  AND (c.companyId IS NOT NULL OR p.postId IS NOT NULL)
		ORDER BY f.createdAt DESC, f.favoriteId DESC
		""",
		countQuery = """
		SELECT COUNT(f)
		FROM Favorite f
		LEFT JOIN Company c ON c.companyId = f.companyId AND c.deletedAt IS NULL AND c.visibility = :companyVisibility
		LEFT JOIN Post p ON p.postId = f.postId AND p.deletedAt IS NULL AND p.visibility = :postVisibility
		WHERE f.memberId = :memberId
		  AND (:targetType IS NULL OR f.targetType = :targetType)
		  AND (c.companyId IS NOT NULL OR p.postId IS NOT NULL)
		""")
	Page<FavoriteResponse> findCards(@Param("memberId") Long memberId,
		@Param("targetType") FavoriteTargetType targetType,
		@Param("companyVisibility") CompanyVisibility companyVisibility,
		@Param("postVisibility") PostVisibility postVisibility, Pageable pageable);
}
