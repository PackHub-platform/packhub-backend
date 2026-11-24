package com.example.packhub.domain.post.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 게시글 사진·첨부파일 (글당 최대 10개). 파일은 업로드 API로 먼저 올린 것만 연결한다 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostAttachment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long postAttachmentId;

	@Column(nullable = false)
	private Long postId;

	@Column(nullable = false)
	private Long fileId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AttachType attachType;

	@Column(nullable = false)
	private int sortOrder;

	public static PostAttachment of(Long postId, Long fileId, AttachType attachType, int sortOrder) {
		PostAttachment attachment = new PostAttachment();
		attachment.postId = postId;
		attachment.fileId = fileId;
		attachment.attachType = attachType;
		attachment.sortOrder = sortOrder;
		return attachment;
	}
}
