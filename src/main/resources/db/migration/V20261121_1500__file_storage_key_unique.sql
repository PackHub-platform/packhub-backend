-- 같은 저장 키로 업로드 완료를 두 번 알려도 File이 하나만 생기도록 중복 금지 (2026-11-21 결정)
ALTER TABLE `File` ADD CONSTRAINT `UK_FILE_STORAGEKEY` UNIQUE (`storageKey`);
