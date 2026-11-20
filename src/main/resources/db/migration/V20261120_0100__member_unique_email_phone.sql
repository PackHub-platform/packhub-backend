-- 같은 이메일·휴대폰으로 동시에 가입 요청이 들어와도 한 명만 저장되도록 DB에서 막는다.
-- 탈퇴하면 이메일·휴대폰을 withdrawn_{id} 형태로 마스킹하므로 탈퇴 회원과 겹치지 않는다.
ALTER TABLE `Member` ADD CONSTRAINT `UK_Member_email` UNIQUE (`email`);
ALTER TABLE `Member` ADD CONSTRAINT `UK_Member_phone` UNIQUE (`phone`);
