-- Refresh Token 저장 (2026-11-20 결정: Redis 대신 MySQL)
-- 로그인 1회 = 1행. 원문 토큰은 저장하지 않고 SHA-256 해시(hex 64자)만 저장한다.
-- 재발급(회전)·로그아웃 때 revokedAt을 채워서 다시 못 쓰게 한다.
CREATE TABLE `RefreshToken` (
	`refreshTokenId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`tokenHash`	VARCHAR(64)	NOT NULL	COMMENT 'Refresh Token 원문의 SHA-256 해시',
	`expiresAt`	DATETIME	NOT NULL	COMMENT '발급 후 14일',
	`revokedAt`	DATETIME	NULL	COMMENT '재발급·로그아웃으로 폐기된 시각',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`refreshTokenId`),
	CONSTRAINT `UK_RefreshToken_tokenHash` UNIQUE (`tokenHash`),
	INDEX `IDX_RefreshToken_memberId` (`memberId`)
);
