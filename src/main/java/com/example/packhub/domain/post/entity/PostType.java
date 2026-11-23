package com.example.packhub.domain.post.entity;

/**
 * 게시글 종류. 구인구직·중고장터는 탭이 둘이라 종류도 둘씩이다 (업체탐색은 게시글이 아니다).
 * 종류마다 전용 테이블(PostRequest 등)이 1:1로 붙는다
 */
public enum PostType {
	REQUEST,
	JOB_OFFER,
	JOB_SEEK,
	TRADE_SELL,
	TRADE_BUY,
	OUTSOURCING
}
