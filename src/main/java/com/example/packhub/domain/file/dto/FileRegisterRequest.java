package com.example.packhub.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FileRegisterRequest(
	@NotBlank @Size(max = 500) String storageKey,
	@NotBlank @Size(max = 255) String originalName
) {
}
