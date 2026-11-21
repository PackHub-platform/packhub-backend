package com.example.packhub.domain.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

class FilePurposeTest {

	private static final long MB = 1024 * 1024;

	@Test
	void 이미지_용도는_JPG_PNG를_5MB까지_허용한다() {
		assertThatCode(() -> FilePurpose.PROFILE_IMAGE.validate("image/png", 5 * MB)).doesNotThrowAnyException();
		assertThatCode(() -> FilePurpose.COMPANY_IMAGE.validate("image/jpeg", 1)).doesNotThrowAnyException();
	}

	@Test
	void 이미지_용도에_PDF는_허용하지_않는다() {
		assertThatThrownBy(() -> FilePurpose.PROFILE_IMAGE.validate("application/pdf", MB))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE_TYPE);
	}

	@Test
	void 이미지_용도에서_5MB를_넘으면_거부한다() {
		assertThatThrownBy(() -> FilePurpose.POST_IMAGE.validate("image/png", 5 * MB + 1))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
	}

	@Test
	void 크기가_0이면_거부한다() {
		assertThatThrownBy(() -> FilePurpose.POST_IMAGE.validate("image/png", 0))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
	}

	@Test
	void 사업자등록증은_PDF를_10MB까지_허용하고_넘으면_거부한다() {
		assertThatCode(() -> FilePurpose.BUSINESS_CERTIFICATE.validate("application/pdf", 10 * MB))
			.doesNotThrowAnyException();
		assertThatThrownBy(() -> FilePurpose.BUSINESS_CERTIFICATE.validate("application/pdf", 10 * MB + 1))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
	}

	@Test
	void 문서_용도도_허용되지_않은_형식은_거부한다() {
		assertThatThrownBy(() -> FilePurpose.POST_DOCUMENT.validate("application/zip", MB))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE_TYPE);
	}
}
