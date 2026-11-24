package com.example.packhub.domain.post.repository;

import com.example.packhub.domain.post.entity.PostAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

	/** 화면에 보여 줄 순서대로 */
	List<PostAttachment> findByPostIdOrderBySortOrderAsc(Long postId);

	/** 수정할 때 기존 첨부를 지운다. 새 행을 넣기 전에 바로 실행되도록 쿼리로 지운다 */
	@Modifying
	@Query("DELETE FROM PostAttachment a WHERE a.postId = :postId")
	void deleteByPostId(@Param("postId") Long postId);
}
