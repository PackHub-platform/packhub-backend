package com.example.packhub.domain.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.service.StorageKey;
import org.junit.jupiter.api.Test;

class StorageKeyTest {

	@Test
	void 키는_용도폴더_회원ID_UUID_확장자_형식이다() {
		String key = StorageKey.create(FilePurpose.COMPANY_IMAGE, 12L, "image/png");

		assertThat(key).matches("company-image/12/[0-9a-f-]{36}\\.png");
	}

	@Test
	void 확장자는_contentType으로_정한다() {
		assertThat(StorageKey.create(FilePurpose.PROFILE_IMAGE, 1L, "image/jpeg")).endsWith(".jpg");
		assertThat(StorageKey.create(FilePurpose.BUSINESS_CERTIFICATE, 1L, "application/pdf")).endsWith(".pdf");
	}

	@Test
	void 매번_다른_키를_만든다() {
		assertThat(StorageKey.create(FilePurpose.POST_IMAGE, 1L, "image/png"))
			.isNotEqualTo(StorageKey.create(FilePurpose.POST_IMAGE, 1L, "image/png"));
	}

	@Test
	void 본인이_발급받은_키는_본인_것이다() {
		String key = StorageKey.create(FilePurpose.COMPANY_LOGO, 7L, "image/png");

		assertThat(StorageKey.isOwnedBy(key, 7L)).isTrue();
	}

	@Test
	void 다른_회원의_키는_본인_것이_아니다() {
		String key = StorageKey.create(FilePurpose.COMPANY_LOGO, 7L, "image/png");

		assertThat(StorageKey.isOwnedBy(key, 8L)).isFalse();
		assertThat(StorageKey.isOwnedBy(key, 77L)).isFalse();
	}

	@Test
	void 모르는_폴더나_형식이_이상한_키는_본인_것이_아니다() {
		assertThat(StorageKey.isOwnedBy("unknown-folder/7/a.png", 7L)).isFalse();
		assertThat(StorageKey.isOwnedBy("company-logo/7", 7L)).isFalse();
		assertThat(StorageKey.isOwnedBy("company-logo/7/", 7L)).isFalse();
		assertThat(StorageKey.isOwnedBy("company-logo/7/a/b.png", 7L)).isFalse();
		assertThat(StorageKey.isOwnedBy("../company-logo/7/a.png", 7L)).isFalse();
	}
}
