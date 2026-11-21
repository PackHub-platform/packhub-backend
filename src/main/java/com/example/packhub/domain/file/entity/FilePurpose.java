package com.example.packhub.domain.file.entity;

import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 업로드 용도별 저장 폴더, 허용 형식, 최대 크기. 기능명세서와 다르면 명세서를 따른다 */
@Getter
@RequiredArgsConstructor
public enum FilePurpose {

	PROFILE_IMAGE("profile-image", Kind.IMAGE),
	COMPANY_LOGO("company-logo", Kind.IMAGE),
	COMPANY_BACKGROUND("company-background", Kind.IMAGE),
	COMPANY_IMAGE("company-image", Kind.IMAGE),
	COMPANY_EQUIPMENT("company-equipment", Kind.IMAGE),
	COMPANY_CERTIFICATION("company-certification", Kind.IMAGE),
	POST_IMAGE("post-image", Kind.IMAGE),
	BUSINESS_CERTIFICATE("business-certificate", Kind.DOCUMENT),
	POST_DOCUMENT("post-document", Kind.DOCUMENT);

	private static final long MB = 1024 * 1024;

	private final String folder;
	private final Kind kind;

	public static Optional<FilePurpose> fromFolder(String folder) {
		return Arrays.stream(values()).filter(purpose -> purpose.folder.equals(folder)).findFirst();
	}

	/** 형식이 맞지 않으면 400 INVALID_FILE_TYPE, 크기가 0 이하이거나 넘으면 400 FILE_TOO_LARGE */
	public void validate(String contentType, long sizeBytes) {
		if (!kind.contentTypes.contains(contentType)) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}
		if (sizeBytes <= 0 || sizeBytes > kind.maxBytes) {
			throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
		}
	}

	@RequiredArgsConstructor
	enum Kind {
		IMAGE(Set.of("image/jpeg", "image/png"), 5 * MB),
		DOCUMENT(Set.of("application/pdf", "image/jpeg", "image/png"), 10 * MB);

		private final Set<String> contentTypes;
		private final long maxBytes;
	}
}
