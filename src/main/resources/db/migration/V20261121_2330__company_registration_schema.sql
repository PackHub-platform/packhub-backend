-- 업체 등록에 맞춘 스키마 변경 (2026-11-21 결정). ERD 파일·ERDCloud에는 아직 미반영.

-- 생산품목: 카테고리·업종과 별개로 직접 입력하는 값. 직원 수는 필수, 홈페이지는 선택으로 바꾼다
ALTER TABLE `Company`
	ADD COLUMN `products` VARCHAR(255) NOT NULL COMMENT '생산품목(직접 입력)' AFTER `industry`,
	MODIFY COLUMN `employeeCount` INT NOT NULL COMMENT '직원 수(규모순 정렬)',
	MODIFY COLUMN `homepageUrl` VARCHAR(255) NULL;

-- 보유 인증: 인증명을 직접 적지 않고 정해진 목록(CertificationType)에서 고른다. 개수 제한은 없다
ALTER TABLE `CompanyCertification`
	DROP COLUMN `name`,
	ADD COLUMN `certType` VARCHAR(30) NOT NULL COMMENT '인증 종류 코드(CertificationType)' AFTER `companyId`,
	MODIFY COLUMN `sortOrder` INT NOT NULL COMMENT '정렬 순서',
	ADD CONSTRAINT `UK_COMPANYCERTIFICATION_COMPANY_CERTTYPE` UNIQUE (`companyId`, `certType`);

-- 업체·의뢰·외주는 같은 분류 목록을 쓰므로 공용 값 COMMON 하나로 둔다
ALTER TABLE `Category`
	MODIFY COLUMN `domain` VARCHAR(30) NOT NULL COMMENT '사용처: COMMON(업체·의뢰·외주 공용), JOB, TRADE';

ALTER TABLE `CompanyCategory`
	MODIFY COLUMN `categoryId` BIGINT NOT NULL COMMENT '카테고리(소분류). 업체당 1개, 여러 개 허용은 논의 예정';
