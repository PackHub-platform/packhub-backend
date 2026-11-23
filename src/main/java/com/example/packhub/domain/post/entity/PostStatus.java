package com.example.packhub.domain.post.entity;

/** 작성자가 바꾸는 진행 상태. 임시저장(DRAFT)은 쓰지 않는다 (2026-11-23 결정) */
public enum PostStatus {
	OPEN,
	CLOSED
}
