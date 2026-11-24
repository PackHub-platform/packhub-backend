package com.example.packhub.domain.post.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모든 서비스(의뢰·구인구직·중고장터·외주)가 같이 쓰는 게시글 공통 정보.
 * 서비스별 전용 정보는 같은 postId를 PK로 쓰는 전용 테이블(PostRequest 등)에 저장한다.
 * 삭제는 deletedAt만 채운다 (전용 테이블·첨부는 그대로 둔다).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long postId;

	@Column(nullable = false)
	private Long memberId;

	/** 작성 업체. 개인도 쓰는 서비스(구인구직 등)에서는 비어 있을 수 있다 */
	private Long companyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PostType postType;

	private Long categoryId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 1500)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PostStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PostVisibility visibility;

	/** "개인정보 공개" 토글. 끄면 상세에서 담당자·연락처·이메일·연락시간을 숨긴다 */
	@Column(nullable = false)
	private boolean isContactPublic;

	/**
	 * 글마다 따로 적는 담당자 연락처 칸. 등록 화면에 입력칸이 없어서 지금은 채우지 않고,
	 * 상세 조회 때 작성자(Member) 정보를 읽어 보여 준다 (2026-11-23 결정, PM 확인 중)
	 */
	@Column(length = 50)
	private String contactName;

	@Column(length = 20)
	private String contactPhone;

	@Column(length = 255)
	private String contactEmail;

	@Column(length = 50)
	private String contactHours;

	@Column(nullable = false)
	private int viewCount;

	private LocalDateTime closedAt;

	private LocalDateTime deletedAt;

	/** 등록. 바로 게시(OPEN)·노출(VISIBLE) 상태, 조회수 0으로 시작한다 */
	@Builder
	private Post(Long memberId, Long companyId, PostType postType, Long categoryId, String title, String content,
		boolean isContactPublic) {
		this.memberId = memberId;
		this.companyId = companyId;
		this.postType = postType;
		this.categoryId = categoryId;
		this.title = title;
		this.content = content;
		this.isContactPublic = isContactPublic;
		this.status = PostStatus.OPEN;
		this.visibility = PostVisibility.VISIBLE;
		this.viewCount = 0;
	}
}
