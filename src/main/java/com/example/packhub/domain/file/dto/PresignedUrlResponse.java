package com.example.packhub.domain.file.dto;

import java.time.LocalDateTime;

public record PresignedUrlResponse(
	String uploadUrl,
	String storageKey,
	LocalDateTime expiresAt
) {
}
