-- 결제 요청 단계에서 Payment를 PENDING(결제 대기) 주문으로 저장하기 위한 변경 (2026-11-19 결정)
-- 구독·이용기간은 결제 승인 때 채우므로 NULL 허용, 승인 때 이용기간 계산에 필요한 결제 주기 추가, 주문번호 중복 금지
ALTER TABLE `Payment`
	MODIFY `subscriptionId` BIGINT NULL COMMENT '결제 완료 시 채움',
	MODIFY `periodStart` DATE NULL COMMENT '이 결제로 늘어난 이용기간 시작 (결제 완료 시 채움)',
	MODIFY `periodEnd` DATE NULL COMMENT '이 결제로 늘어난 이용기간 끝 (결제 완료 시 채움)',
	ADD COLUMN `billingCycle` VARCHAR(30) NOT NULL COMMENT '결제 주기: MONTHLY, YEARLY' AFTER `planNameSnapshot`,
	ADD CONSTRAINT `UK_PAYMENT_ORDERNO` UNIQUE (`orderNo`);
