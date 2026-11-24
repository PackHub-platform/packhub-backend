-- 이메일·휴대폰 인증번호 (2026-11-24 결정: Redis 대신 MySQL, 테이블은 V1의 VerificationCode)
-- 발송·확인·가입 때 "이 수단·대상의 가장 최근 인증번호"를 찾으므로 (channel, target)으로 찾는 인덱스를 둔다.
CREATE INDEX `IDX_VerificationCode_channel_target` ON `VerificationCode` (`channel`, `target`);
