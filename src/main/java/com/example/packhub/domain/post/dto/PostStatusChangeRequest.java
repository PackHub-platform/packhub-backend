package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.PostStatus;
import jakarta.validation.constraints.NotNull;

/** 마감·재게시 요청 (내 게시글 관리 10-1의 "마감/모집중" 토글) */
public record PostStatusChangeRequest(@NotNull PostStatus status) {
}
