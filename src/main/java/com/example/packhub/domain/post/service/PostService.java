package com.example.packhub.domain.post.service;

import com.example.packhub.domain.post.dto.MyPostPageResponse;
import com.example.packhub.domain.post.dto.MyPostSearchRequest;
import com.example.packhub.domain.post.dto.PostStatusResponse;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.repository.PostRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모든 게시글 종류(의뢰·구인구직·중고장터·외주)가 같이 쓰는 기능: 마감·재게시, 삭제, 내 게시글 목록.
 * 바꾸는 기능은 작성자만 할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private final PostRepository postRepository;

	/**
	 * 마감(OPEN → CLOSED)·재게시(CLOSED → OPEN). 지금과 같은 상태로 바꾸려 하면 409 INVALID_POST_STATUS_TRANSITION.
	 * 없거나 삭제된 글은 404 POST_NOT_FOUND, 남의 글은 403 FORBIDDEN.
	 */
	@Transactional
	public PostStatusResponse changeStatus(Long memberId, Long postId, PostStatus status) {
		Post post = findMine(memberId, postId);
		if (post.getStatus() == status) {
			throw new BusinessException(ErrorCode.INVALID_POST_STATUS_TRANSITION);
		}
		post.changeStatus(status, LocalDateTime.now());
		return PostStatusResponse.from(post);
	}

	/** 삭제 표시(deletedAt)만 한다. 없거나 이미 삭제된 글은 404, 남의 글은 403 */
	@Transactional
	public void delete(Long memberId, Long postId) {
		findMine(memberId, postId).delete(LocalDateTime.now());
	}

	/** 내 게시글 (10-1). 삭제한 글은 빼고, 관리자가 숨긴 글은 visibility로 알려 준다 */
	public MyPostPageResponse getMyPosts(Long memberId, MyPostSearchRequest request) {
		return MyPostPageResponse.of(postRepository.findMyPosts(memberId, request.postType(), request.status(),
			PageRequest.of(request.page() - 1, request.size())));
	}

	/** 작성자가 바꾸는 기능용. 행을 잠그고 찾는다 */
	Post findMine(Long memberId, Long postId) {
		Post post = postRepository.findForUpdateByPostIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		if (!post.isWrittenBy(memberId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return post;
	}
}
