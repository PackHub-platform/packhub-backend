package com.example.packhub.domain.file.service;

import com.example.packhub.domain.file.entity.FilePurpose;
import java.util.Map;
import java.util.UUID;

/**
 * S3 저장 키 규칙: {용도 폴더}/{회원 ID}/{UUID}.{확장자}  예: company-image/12/0c1f...e9.png
 * 키는 서버가 만들고, 확장자는 사용자가 바꿀 수 있는 파일 이름이 아니라 contentType으로 정한다.
 */
public final class StorageKey {

	private static final Map<String, String> EXTENSIONS = Map.of(
		"image/jpeg", "jpg",
		"image/png", "png",
		"application/pdf", "pdf");

	private StorageKey() {
	}

	/** contentType은 FilePurpose.validate로 먼저 검사한다 */
	public static String create(FilePurpose purpose, Long memberId, String contentType) {
		return purpose.getFolder() + "/" + memberId + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
	}

	/** 첫 칸이 알려진 용도 폴더이고 두 번째 칸이 본인 회원 ID인지 확인한다 */
	public static boolean isOwnedBy(String storageKey, Long memberId) {
		String[] parts = storageKey.split("/");
		return parts.length == 3
			&& FilePurpose.fromFolder(parts[0]).isPresent()
			&& parts[1].equals(String.valueOf(memberId))
			&& !parts[2].isBlank();
	}
}
