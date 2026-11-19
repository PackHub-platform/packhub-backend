-- 이용권 플랜 기초 데이터 (기능명세서 11-1 기준). 명칭·연간 결제 지원 여부는 기획 확인 중.
INSERT INTO `Plan` (`code`, `name`, `description`, `monthlyPrice`, `yearlyPrice`, `isConsultOnly`, `isActive`)
VALUES
	('BASIC', 'PackHub Basic', '기업페이지 기본 기능', 29000, 290000, FALSE, TRUE),
	('BOOST', 'PackHub Boost', '검색·카테고리 노출 강화', 79000, 790000, FALSE, TRUE),
	('PARTNER', 'PackHub Partner', '맞춤 견적, 최소 3개월 (50만원부터)', NULL, NULL, TRUE, TRUE);
