package com.example.packhub.domain.post.repository;

import com.example.packhub.domain.post.dto.RequestSummaryResponse;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostType;
import com.example.packhub.domain.post.entity.PostVisibility;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	/** 삭제하지 않았고 노출 상태가 맞는 게시글 (종류까지 맞아야 한다) */
	Optional<Post> findByPostIdAndPostTypeAndVisibilityAndDeletedAtIsNull(Long postId, PostType postType,
		PostVisibility visibility);

	/**
	 * 작성자가 바꾸는 기능(수정·상태 변경·삭제)용. 삭제하지 않은 글이면 숨김 상태여도 찾는다.
	 * 같은 글에 요청이 동시에 와도 차례대로 처리되도록 행을 잠그고 조회한다 (업체 수정과 같은 방식)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Post> findForUpdateByPostIdAndDeletedAtIsNull(Long postId);

	/**
	 * 수정 시각만 갱신한다. 의뢰 전용 칸·첨부만 바뀌면 Post 행이 바뀌지 않아 updatedAt이 자동으로 갱신되지 않기 때문에 쓴다
	 */
	@Modifying
	@Query("UPDATE Post p SET p.updatedAt = :now WHERE p.postId = :postId")
	void touch(@Param("postId") Long postId, @Param("now") LocalDateTime now);

	/**
	 * 내 게시글 (마이페이지 10-1). 삭제한 글은 빼고, 관리자가 숨긴 글은 보여 준다. 최근에 쓴 글 먼저.
	 *
	 * @param postType null이면 모든 종류
	 * @param status   null이면 모든 상태
	 */
	@Query("""
		SELECT p FROM Post p
		WHERE p.memberId = :memberId AND p.deletedAt IS NULL
		  AND (:postType IS NULL OR p.postType = :postType)
		  AND (:status IS NULL OR p.status = :status)
		ORDER BY p.createdAt DESC, p.postId DESC
		""")
	Page<Post> findMyPosts(@Param("memberId") Long memberId, @Param("postType") PostType postType,
		@Param("status") PostStatus status, Pageable pageable);

	/**
	 * 의뢰 목록 카드. 게시 중(OPEN)이고 노출 중이며 삭제하지 않은 의뢰만 찾는다.
	 * 카드에 필요한 값이 세 테이블(Post·PostRequest·Company)에 나뉘어 있어서 postId·companyId로 이어 붙인다.
	 *
	 * @param keyword       의뢰 제목·업체명에 쓸 LIKE 패턴('!'로 이스케이프). null이면 검색어 조건 없음
	 * @param allCategories true면 카테고리 조건 없음
	 * @param categoryIds   의뢰의 카테고리(소분류)가 이 중 하나여야 한다
	 * @param sort          RequestSort 이름. 납기 정렬이면 희망납기가 today보다 앞선(지난) 의뢰를 맨 뒤로 보낸다
	 */
	@Query(value = """
		SELECT new com.example.packhub.domain.post.dto.RequestSummaryResponse(
		       p.postId, p.title, c.industry, c.name, pr.quantity, pr.quantityUnit, pr.desiredDeliveryDate, p.createdAt)
		FROM Post p
		JOIN PostRequest pr ON pr.postId = p.postId
		JOIN Company c ON c.companyId = p.companyId
		WHERE p.postType = com.example.packhub.domain.post.entity.PostType.REQUEST
		  AND p.status = com.example.packhub.domain.post.entity.PostStatus.OPEN
		  AND p.visibility = com.example.packhub.domain.post.entity.PostVisibility.VISIBLE
		  AND p.deletedAt IS NULL
		  AND (:keyword IS NULL OR p.title LIKE :keyword ESCAPE '!' OR c.name LIKE :keyword ESCAPE '!')
		  AND (:allCategories = TRUE OR p.categoryId IN :categoryIds)
		ORDER BY
		  CASE WHEN :sort <> 'LATEST' AND pr.desiredDeliveryDate < :today THEN 1 ELSE 0 END,
		  CASE WHEN :sort = 'DELIVERY_ASC' THEN pr.desiredDeliveryDate END ASC,
		  CASE WHEN :sort = 'DELIVERY_DESC' THEN pr.desiredDeliveryDate END DESC,
		  p.createdAt DESC, p.postId DESC
		""",
		countQuery = """
		SELECT COUNT(p)
		FROM Post p
		JOIN Company c ON c.companyId = p.companyId
		WHERE p.postType = com.example.packhub.domain.post.entity.PostType.REQUEST
		  AND p.status = com.example.packhub.domain.post.entity.PostStatus.OPEN
		  AND p.visibility = com.example.packhub.domain.post.entity.PostVisibility.VISIBLE
		  AND p.deletedAt IS NULL
		  AND (:keyword IS NULL OR p.title LIKE :keyword ESCAPE '!' OR c.name LIKE :keyword ESCAPE '!')
		  AND (:allCategories = TRUE OR p.categoryId IN :categoryIds)
		""")
	Page<RequestSummaryResponse> searchRequests(@Param("keyword") String keyword,
		@Param("allCategories") boolean allCategories, @Param("categoryIds") Collection<Long> categoryIds,
		@Param("sort") String sort, @Param("today") LocalDate today, Pageable pageable);
}
