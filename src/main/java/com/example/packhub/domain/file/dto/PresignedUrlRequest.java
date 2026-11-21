package com.example.packhub.domain.file.dto;

import com.example.packhub.domain.file.entity.FilePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** fileName은 받기만 한다. 저장 키의 확장자는 contentType으로 정한다 */
public record PresignedUrlRequest(
	@NotNull FilePurpose purpose,
	@NotBlank @Size(max = 255) String fileName,
	@NotBlank String contentType,
	@NotNull @Positive Long sizeBytes
) {
}
