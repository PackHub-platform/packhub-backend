-- ERD(PACKHUB.sql) 기반 초기 스키마. PK를 CREATE TABLE 안으로 옮기고 AUTO_INCREMENT를 추가함.

CREATE TABLE `Post` (
	`postId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`companyId`	BIGINT	NULL	COMMENT '작성 업체(기업회원 글)',
	`postType`	VARCHAR(30)	NOT NULL	COMMENT '종류: REQUEST, JOB_OFFER, JOB_SEEK, TRADE_SELL, TRADE_BUY, OUTSOURCING',
	`categoryId`	BIGINT	NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`content`	VARCHAR(1500)	NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '진행 상태: DRAFT, OPEN(모집중), CLOSED(마감)',
	`visibility`	VARCHAR(30)	NOT NULL	COMMENT '노출 여부: VISIBLE, HIDDEN',
	`isContactPublic`	BOOLEAN	NOT NULL,
	`contactName`	VARCHAR(50)	NULL	COMMENT '담당자(작성 시점 스냅샷)',
	`contactPhone`	VARCHAR(20)	NULL	COMMENT '연락처(스냅샷)',
	`contactEmail`	VARCHAR(255)	NULL	COMMENT '이메일(스냅샷)',
	`contactHours`	VARCHAR(50)	NULL	COMMENT '연락시간(스냅샷)',
	`viewCount`	INT	NOT NULL	DEFAULT 0	COMMENT '조회수(원자적 UPDATE)',
	`closedAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`postId`)
);

CREATE TABLE `PostJob` (
	`postId`	BIGINT	NOT NULL	COMMENT '게시글 ID(1:1, 구인·구직 공용)',
	`jobRole`	VARCHAR(100)	NULL	COMMENT '직무 / 희망직무',
	`employmentType`	VARCHAR(30)	NULL	COMMENT '고용형태: FULL_TIME, CONTRACT, PART_TIME, ETC',
	`salaryText`	VARCHAR(100)	NULL	COMMENT '급여 / 희망금액(표시용 문구)',
	`workRegion`	VARCHAR(100)	NULL	COMMENT '지역 / 희망지역',
	`careerYears`	INT	NULL	COMMENT '경력(년)',
	`startDateText`	VARCHAR(50)	NULL	COMMENT '출근일자(표시용 문구)',
	`shiftWork`	VARCHAR(50)	NULL,
	PRIMARY KEY (`postId`)
);

CREATE TABLE `MemberStatusHistory` (
	`memberStatusHistoryId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`fromStatus`	VARCHAR(30)	NOT NULL,
	`toStatus`	VARCHAR(30)	NOT NULL,
	`reason`	VARCHAR(500)	NOT NULL,
	`changedBy`	BIGINT	NOT NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`memberStatusHistoryId`)
);

CREATE TABLE `PostRequest` (
	`postId`	BIGINT	NOT NULL	COMMENT '게시글 ID(1:1)',
	`item`	VARCHAR(100)	NULL	COMMENT '품목 ⚠️ 등록 화면 입력란 확인 중',
	`quantity`	INT	NULL,
	`quantityUnit`	VARCHAR(10)	NULL	COMMENT '수량 단위(매, 롤 등)',
	`desiredPrice`	BIGINT	NULL	COMMENT '희망 가격(원)',
	`packageType`	VARCHAR(100)	NULL,
	`contents`	VARCHAR(100)	NULL,
	`spec`	VARCHAR(100)	NULL,
	`materialThickness`	VARCHAR(100)	NULL,
	`printSpec`	VARCHAR(100)	NULL,
	`materialComposition`	VARCHAR(200)	NULL,
	`artworkProvided`	VARCHAR(50)	NULL,
	`existingSample`	VARCHAR(50)	NULL,
	`desiredDeliveryDate`	DATE	NULL	COMMENT '희망납기(NULL이면 협의)',
	`deadlineDate`	DATE	NULL,
	`deliveryRegion`	VARCHAR(100)	NULL,
	`deliveryMethod`	VARCHAR(100)	NULL,
	`priceTerms`	VARCHAR(100)	NULL,
	`paymentTerms`	VARCHAR(100)	NULL,
	PRIMARY KEY (`postId`)
);

CREATE TABLE `Inquiry` (
	`inquiryId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`inquiryType`	VARCHAR(30)	NOT NULL	COMMENT '문의 유형 ⚠️ 값 목록 확인 중',
	`replyEmail`	VARCHAR(255)	NOT NULL,
	`title`	VARCHAR(200)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`attachmentFileId`	BIGINT	NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '상태: WAITING, ANSWERED',
	`answerContent`	TEXT	NULL,
	`answeredBy`	BIGINT	NULL,
	`answeredAt`	DATETIME	NULL,
	`privacyAgreedAt`	DATETIME	NOT NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`inquiryId`)
);

CREATE TABLE `BusinessVerification` (
	`businessVerificationId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL	COMMENT '신청 회원(재신청 가능, 1:N)',
	`businessName`	VARCHAR(100)	NOT NULL,
	`representativeName`	VARCHAR(50)	NOT NULL,
	`businessNumber`	VARCHAR(12)	NOT NULL,
	`certificateFileId`	BIGINT	NOT NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '인증 상태: PENDING, APPROVED, REJECTED ⚠️ 자동/관리자 검토 확인 중',
	`rejectReason`	VARCHAR(500)	NULL,
	`reviewedBy`	BIGINT	NULL,
	`reviewedAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`businessVerificationId`)
);

CREATE TABLE `Category` (
	`categoryId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`domain`	VARCHAR(30)	NOT NULL	COMMENT '사용처: COMPANY, REQUEST, JOB, TRADE, OUTSOURCING',
	`parentId`	BIGINT	NULL,
	`name`	VARCHAR(50)	NOT NULL,
	`sortOrder`	INT	NOT NULL,
	`isActive`	BOOLEAN	NOT NULL	DEFAULT TRUE,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`categoryId`)
);

CREATE TABLE `Favorite` (
	`favoriteId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`targetType`	VARCHAR(30)	NOT NULL	COMMENT '대상 종류: COMPANY, POST',
	`companyId`	BIGINT	NULL	COMMENT '업체(targetType=COMPANY일 때만)',
	`postId`	BIGINT	NULL	COMMENT '게시글(targetType=POST일 때만)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`favoriteId`)
);

CREATE TABLE `PostJobQualification` (
	`postJobQualificationId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`postId`	BIGINT	NOT NULL,
	`qualType`	VARCHAR(30)	NOT NULL	COMMENT '종류: SKILL, CERTIFICATE',
	`content`	VARCHAR(200)	NOT NULL,
	`sortOrder`	INT	NOT NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`postJobQualificationId`)
);

CREATE TABLE `CompanyCertification` (
	`companyCertificationId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`companyId`	BIGINT	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`imageFileId`	BIGINT	NULL,
	`sortOrder`	INT	NOT NULL	COMMENT '정렬 순서(최대 20개)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`companyCertificationId`)
);

CREATE TABLE `Terms` (
	`termsId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`termsType`	VARCHAR(30)	NOT NULL	COMMENT '약관 종류: SERVICE, PRIVACY, AGE_14, LOCATION, MARKETING, PAYMENT_REFUND, RECURRING_PAYMENT, INQUIRY_PRIVACY, CONSULT_PRIVACY',
	`version`	VARCHAR(20)	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`isRequired`	BOOLEAN	NOT NULL,
	`effectiveAt`	DATETIME	NOT NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`termsId`)
);

CREATE TABLE `File` (
	`fileId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`uploaderId`	BIGINT	NOT NULL	COMMENT '업로드한 회원',
	`originalName`	VARCHAR(255)	NOT NULL,
	`storageKey`	VARCHAR(500)	NOT NULL	COMMENT '저장소 키(S3 key 등)',
	`contentType`	VARCHAR(100)	NOT NULL,
	`sizeBytes`	BIGINT	NOT NULL	COMMENT '파일 크기(byte)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`fileId`)
);

CREATE TABLE `PartnerConsultation` (
	`partnerConsultationId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`companyName`	VARCHAR(100)	NOT NULL,
	`managerName`	VARCHAR(50)	NOT NULL,
	`phone`	VARCHAR(20)	NOT NULL,
	`email`	VARCHAR(255)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '상태: RECEIVED, IN_PROGRESS, DONE',
	`privacyAgreedAt`	DATETIME	NOT NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`partnerConsultationId`)
);

CREATE TABLE `ChatParticipant` (
	`chatParticipantId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`chatRoomId`	BIGINT	NOT NULL,
	`memberId`	BIGINT	NOT NULL,
	`isInfoPublic`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`notificationEnabled`	BOOLEAN	NOT NULL	DEFAULT TRUE,
	`lastReadMessageId`	BIGINT	NULL	COMMENT '마지막으로 읽은 메시지(안 읽은 수 계산)',
	`leftAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`chatParticipantId`)
);

CREATE TABLE `Report` (
	`reportId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`reporterId`	BIGINT	NOT NULL,
	`targetType`	VARCHAR(30)	NOT NULL	COMMENT '대상 종류: COMPANY, POST, MEMBER, CHAT_ROOM',
	`targetId`	BIGINT	NOT NULL	COMMENT '대상 ID(다형 참조, FK 없음)',
	`targetSnapshot`	VARCHAR(200)	NULL	COMMENT '신고 당시 대상 정보(스냅샷)',
	`reason`	VARCHAR(30)	NOT NULL	COMMENT '신고 사유 ⚠️ 값 목록 확인 중',
	`content`	TEXT	NOT NULL,
	`evidenceFileId`	BIGINT	NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '처리 상태: PENDING, RESOLVED, REJECTED',
	`action`	VARCHAR(30)	NULL	COMMENT '처리 조치: NONE, HIDE, DELETE, SUSPEND_MEMBER',
	`adminMemo`	VARCHAR(1000)	NULL,
	`handledBy`	BIGINT	NULL,
	`handledAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`reportId`)
);

CREATE TABLE `VerificationCode` (
	`verificationCodeId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`channel`	VARCHAR(30)	NOT NULL	COMMENT '인증 수단: EMAIL, PHONE',
	`target`	VARCHAR(255)	NOT NULL	COMMENT '이메일 또는 휴대폰 번호',
	`codeHash`	VARCHAR(255)	NOT NULL,
	`expiresAt`	DATETIME	NOT NULL,
	`attemptCount`	INT	NOT NULL	DEFAULT 0,
	`verifiedAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`verificationCodeId`)
);

CREATE TABLE `Faq` (
	`faqId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`category`	VARCHAR(30)	NULL,
	`question`	VARCHAR(200)	NOT NULL,
	`answer`	TEXT	NOT NULL,
	`sortOrder`	INT	NOT NULL,
	`isVisible`	BOOLEAN	NOT NULL	DEFAULT TRUE,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`faqId`)
);

CREATE TABLE `PostTrade` (
	`postId`	BIGINT	NOT NULL	COMMENT '게시글 ID(1:1, 팔기·사기 공용)',
	`price`	BIGINT	NULL	COMMENT '가격(원)',
	`itemCondition`	VARCHAR(30)	NULL	COMMENT '상품 상태 ⚠️ 값 목록 확인 중(예: NEW_LIKE)',
	`quantity`	INT	NULL,
	`tradeRegion`	VARCHAR(100)	NULL,
	`tradeMethod`	VARCHAR(100)	NULL,
	`tradeTerms`	VARCHAR(200)	NULL,
	`inspection`	VARCHAR(200)	NULL,
	`shippingTerms`	VARCHAR(200)	NULL,
	PRIMARY KEY (`postId`)
);

CREATE TABLE `ChatMessage` (
	`chatMessageId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`chatRoomId`	BIGINT	NOT NULL,
	`senderId`	BIGINT	NULL	COMMENT '보낸 회원(시스템 메시지는 NULL)',
	`messageType`	VARCHAR(30)	NOT NULL	COMMENT '종류: TEXT, FILE, SYSTEM',
	`content`	TEXT	NULL,
	`fileId`	BIGINT	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`chatMessageId`)
);

CREATE TABLE `Notice` (
	`noticeId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`authorId`	BIGINT	NOT NULL,
	`title`	VARCHAR(200)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '게시 상태: PUBLISHED, HIDDEN',
	`attachmentFileId`	BIGINT	NULL,
	`viewCount`	INT	NOT NULL	DEFAULT 0,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`noticeId`)
);

CREATE TABLE `Company` (
	`companyId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL	COMMENT '기업회원(1:1)',
	`name`	VARCHAR(100)	NOT NULL,
	`industry`	VARCHAR(50)	NOT NULL,
	`businessHours`	VARCHAR(50)	NOT NULL	COMMENT '영업시간(예: 08:00 - 18:00)',
	`contactPhone`	VARCHAR(20)	NOT NULL,
	`address`	VARCHAR(255)	NOT NULL,
	`addressDetail`	VARCHAR(255)	NULL,
	`latitude`	DECIMAL(10, 7)	NULL	COMMENT '위도(지도)',
	`longitude`	DECIMAL(10, 7)	NULL	COMMENT '경도(지도)',
	`representativeName`	VARCHAR(50)	NOT NULL,
	`foundedDate`	DATE	NOT NULL	COMMENT '설립일(개업일순 정렬)',
	`employeeCount`	INT	NULL	COMMENT '직원 수(규모순 정렬) ⚠️ 등록 화면 입력란 확인 중',
	`homepageUrl`	VARCHAR(255)	NOT NULL,
	`description`	VARCHAR(1500)	NOT NULL,
	`logoFileId`	BIGINT	NULL	COMMENT '로고',
	`backgroundFileId`	BIGINT	NULL	COMMENT '배경사진',
	`viewCount`	INT	NOT NULL	DEFAULT 0	COMMENT '조회수(원자적 UPDATE)',
	`visibility`	VARCHAR(30)	NOT NULL	COMMENT '노출 여부: VISIBLE, HIDDEN',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL,
	PRIMARY KEY (`companyId`)
);

CREATE TABLE `CompanyCategory` (
	`companyCategoryId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`companyId`	BIGINT	NOT NULL,
	`categoryId`	BIGINT	NOT NULL	COMMENT '카테고리(생산품목) ⚠️ 선택 방식 확인 중',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`companyCategoryId`)
);

CREATE TABLE `MemberProfile` (
	`memberId`	BIGINT	NOT NULL	COMMENT '회원 ID(1:1)',
	`nickname`	VARCHAR(30)	NULL,
	`profileImageFileId`	BIGINT	NULL	COMMENT '프로필 사진(JPG·PNG, 최대 5MB)',
	`region`	VARCHAR(50)	NULL,
	`isNamePublic`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`isRegionPublic`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`memberId`)
);

CREATE TABLE `ChatRoom` (
	`chatRoomId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`contextType`	VARCHAR(30)	NOT NULL	COMMENT '시작한 곳: COMPANY, POST',
	`companyId`	BIGINT	NULL	COMMENT '업체 상세에서 시작',
	`postId`	BIGINT	NULL	COMMENT '게시글 상세에서 시작',
	`inquirerId`	BIGINT	NOT NULL	COMMENT '대화를 시작한 회원(같은 대상·같은 회원은 방 하나) ⚠️ 확인 중',
	`lastMessageAt`	DATETIME	NULL	COMMENT '마지막 메시지 시각(반정규화)',
	`lastMessagePreview`	VARCHAR(200)	NULL	COMMENT '마지막 메시지 미리보기(반정규화)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`chatRoomId`)
);

CREATE TABLE `PostOutsourcing` (
	`postId`	BIGINT	NOT NULL	COMMENT '게시글 ID(1:1)',
	`workTarget`	VARCHAR(100)	NULL,
	`requiredProcess`	VARCHAR(100)	NULL,
	`grade1`	VARCHAR(100)	NULL	COMMENT '1급지 ⚠️ 의미 확인 중',
	`grade2`	VARCHAR(100)	NULL	COMMENT '2급지 ⚠️ 의미 확인 중',
	`fabricWidth`	VARCHAR(50)	NULL,
	`workQuantity`	VARCHAR(100)	NULL,
	`suppliedMaterial`	VARCHAR(200)	NULL,
	`finishedForm`	VARCHAR(100)	NULL,
	`unitPriceText`	VARCHAR(100)	NULL	COMMENT '가공단가(표시용 문구)',
	`desiredDeliveryDate`	DATE	NULL	COMMENT '납기예정(NULL이면 협의)',
	`workStartDate`	DATE	NULL,
	`materialReadyDate`	DATE	NULL,
	`materialOrigin`	VARCHAR(100)	NULL,
	`materialTransport`	VARCHAR(100)	NULL,
	`productPickup`	VARCHAR(100)	NULL,
	PRIMARY KEY (`postId`)
);

CREATE TABLE `Subscription` (
	`subscriptionId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL	COMMENT '기업회원(ACTIVE 구독은 1개, 앱에서 검증)',
	`planId`	BIGINT	NOT NULL,
	`status`	VARCHAR(30)	NOT NULL	COMMENT '상태: ACTIVE, CANCEL_SCHEDULED, EXPIRED, PAYMENT_FAILED',
	`grantType`	VARCHAR(30)	NOT NULL	COMMENT '부여 방식: PAID, ADMIN_FREE',
	`billingCycle`	VARCHAR(30)	NOT NULL	COMMENT '결제 주기: MONTHLY, YEARLY',
	`currentPeriodStart`	DATE	NOT NULL	COMMENT '이용기간 시작',
	`currentPeriodEnd`	DATE	NOT NULL	COMMENT '이용기간 끝',
	`nextBillingDate`	DATE	NULL	COMMENT '다음 결제일(해지 예약이면 NULL)',
	`autoRenew`	BOOLEAN	NOT NULL,
	`paymentMethodId`	BIGINT	NULL,
	`pendingPlanId`	BIGINT	NULL	COMMENT '다음 결제일부터 바뀔 이용권',
	`cancelRequestedAt`	DATETIME	NULL,
	`freeUntil`	DATE	NULL	COMMENT '관리자 무료 기간 끝',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`subscriptionId`)
);

CREATE TABLE `CompanyImage` (
	`companyImageId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`companyId`	BIGINT	NOT NULL,
	`imageType`	VARCHAR(30)	NOT NULL	COMMENT '종류: PORTFOLIO, OFFICE',
	`fileId`	BIGINT	NOT NULL,
	`sortOrder`	INT	NOT NULL	COMMENT '정렬 순서(종류별 최대 10장)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`companyImageId`)
);

CREATE TABLE `Payment` (
	`paymentId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`orderNo`	VARCHAR(30)	NOT NULL	COMMENT '주문번호(PH-YYYYMMDD-000001)',
	`memberId`	BIGINT	NOT NULL	COMMENT '결제 회원(탈퇴 후 5년 보관, CASCADE 금지)',
	`subscriptionId`	BIGINT	NOT NULL,
	`planId`	BIGINT	NOT NULL,
	`planNameSnapshot`	VARCHAR(50)	NOT NULL	COMMENT '결제 당시 이용권 이름(스냅샷)',
	`amount`	BIGINT	NOT NULL	COMMENT '결제 금액(원, 서버 계산)',
	`status`	VARCHAR(30)	NOT NULL	COMMENT '상태: PENDING, PAID, FAILED, CANCELED, REFUNDED',
	`paymentMethodId`	BIGINT	NULL,
	`methodSummary`	VARCHAR(50)	NULL	COMMENT '결제수단 요약(스냅샷, 예: 카드 · 1234)',
	`pgTransactionId`	VARCHAR(100)	NULL,
	`periodStart`	DATE	NOT NULL	COMMENT '이 결제로 늘어난 이용기간 시작',
	`periodEnd`	DATE	NOT NULL	COMMENT '이 결제로 늘어난 이용기간 끝',
	`paidAt`	DATETIME	NULL,
	`failureReason`	VARCHAR(255)	NULL,
	`receiptUrl`	VARCHAR(500)	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`paymentId`)
);

CREATE TABLE `PostAttachment` (
	`postAttachmentId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`postId`	BIGINT	NOT NULL,
	`fileId`	BIGINT	NOT NULL,
	`attachType`	VARCHAR(30)	NOT NULL	COMMENT '종류: IMAGE, DOCUMENT',
	`sortOrder`	INT	NOT NULL	COMMENT '정렬 순서(최대 10개)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`postAttachmentId`)
);

CREATE TABLE `PaymentMethod` (
	`paymentMethodId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`pgBillingKey`	VARCHAR(255)	NOT NULL	COMMENT 'PG 빌링키(암호화 저장). 카드번호 원문 저장 금지',
	`cardBrand`	VARCHAR(20)	NULL,
	`cardKind`	VARCHAR(30)	NULL	COMMENT '카드 종류: CREDIT, CHECK',
	`cardLast4`	CHAR(4)	NULL,
	`expiresYm`	CHAR(6)	NULL	COMMENT '만료 연월(YYYYMM)',
	`isPrimary`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deletedAt`	DATETIME	NULL	COMMENT '삭제 시각(결제수단 변경)',
	PRIMARY KEY (`paymentMethodId`)
);

CREATE TABLE `Plan` (
	`planId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`code`	VARCHAR(20)	NOT NULL	COMMENT '코드: BASIC, BOOST, PARTNER',
	`name`	VARCHAR(50)	NOT NULL,
	`description`	VARCHAR(200)	NULL,
	`monthlyPrice`	BIGINT	NULL	COMMENT '월 가격(원). 상담형은 NULL',
	`yearlyPrice`	BIGINT	NULL	COMMENT '연 가격(원)',
	`isConsultOnly`	BOOLEAN	NOT NULL	COMMENT '상담 신청형 여부(Partner)',
	`isActive`	BOOLEAN	NOT NULL	DEFAULT TRUE,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`planId`)
);

CREATE TABLE `MemberTermsAgreement` (
	`memberTermsAgreementId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`memberId`	BIGINT	NOT NULL,
	`termsId`	BIGINT	NOT NULL	COMMENT '약관(버전)',
	`agreed`	BOOLEAN	NOT NULL	COMMENT '동의 여부(선택 약관 거부도 기록)',
	`agreedAt`	DATETIME	NOT NULL	COMMENT '동의·거부 시각',
	`withdrawnAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`memberTermsAgreementId`)
);

CREATE TABLE `CompanyEquipment` (
	`companyEquipmentId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`companyId`	BIGINT	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`imageFileId`	BIGINT	NULL,
	`sortOrder`	INT	NOT NULL	COMMENT '정렬 순서(최대 20개)',
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`companyEquipmentId`)
);

CREATE TABLE `Member` (
	`memberId`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`email`	VARCHAR(255)	NOT NULL	COMMENT '이메일(로그인 ID). 탈퇴 시 withdrawn_{id}@... 로 마스킹',
	`password`	VARCHAR(255)	NOT NULL	COMMENT '비밀번호(BCrypt 해시)',
	`name`	VARCHAR(50)	NULL	COMMENT '이름 ⚠️ 가입 시 받는지 확인 중',
	`phone`	VARCHAR(20)	NOT NULL	COMMENT '휴대폰 번호. 탈퇴 시 마스킹',
	`memberType`	VARCHAR(30)	NOT NULL	COMMENT '회원 유형: GENERAL, COMPANY',
	`role`	VARCHAR(30)	NOT NULL	COMMENT '권한: USER, ADMIN',
	`status`	VARCHAR(30)	NOT NULL	COMMENT '계정 상태: ACTIVE(정상), SUSPENDED(정지), WITHDRAWN(회원탈퇴)',
	`emailVerifiedAt`	DATETIME	NULL,
	`phoneVerifiedAt`	DATETIME	NULL,
	`lastLoginAt`	DATETIME	NULL,
	`withdrawnAt`	DATETIME	NULL,
	`createdAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updatedAt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`memberId`)
);

ALTER TABLE `PostJob` ADD CONSTRAINT `FK_Post_TO_PostJob_1` FOREIGN KEY (
	`postId`
)
REFERENCES `Post` (
	`postId`
);

ALTER TABLE `PostRequest` ADD CONSTRAINT `FK_Post_TO_PostRequest_1` FOREIGN KEY (
	`postId`
)
REFERENCES `Post` (
	`postId`
);

ALTER TABLE `PostTrade` ADD CONSTRAINT `FK_Post_TO_PostTrade_1` FOREIGN KEY (
	`postId`
)
REFERENCES `Post` (
	`postId`
);

ALTER TABLE `MemberProfile` ADD CONSTRAINT `FK_Member_TO_MemberProfile_1` FOREIGN KEY (
	`memberId`
)
REFERENCES `Member` (
	`memberId`
);

ALTER TABLE `PostOutsourcing` ADD CONSTRAINT `FK_Post_TO_PostOutsourcing_1` FOREIGN KEY (
	`postId`
)
REFERENCES `Post` (
	`postId`
);
