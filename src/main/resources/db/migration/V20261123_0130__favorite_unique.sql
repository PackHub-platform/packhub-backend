-- 관심 중복 방지 (2026-11-23 결정). ERD 파일·ERDCloud에는 아직 미반영.

-- 같은 회원이 같은 대상을 두 번 저장하지 못하게 한다. 동시에 같은 요청이 와도 DB가 막는다.
-- 업체 관심은 postId가, 게시글 관심은 companyId가 NULL인데, NULL끼리는 중복으로 보지 않는다
ALTER TABLE `Favorite`
	ADD CONSTRAINT `UK_FAVORITE_MEMBER_COMPANY` UNIQUE (`memberId`, `companyId`),
	ADD CONSTRAINT `UK_FAVORITE_MEMBER_POST` UNIQUE (`memberId`, `postId`);
