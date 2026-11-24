package com.example.packhub.domain.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사업자 인증 신청 (1-4). 사업자등록증은 파일 업로드 API에 BUSINESS_CERTIFICATE 용도로 먼저 올린다.
 *
 * @param businessNumber "000-00-00000" 형식. 형식·검증 숫자는 서비스에서 확인한다
 */
public record BusinessVerificationRequest(
	@NotBlank @Size(max = 100) String businessName,
	@NotBlank @Size(max = 50) String representativeName,
	@NotBlank String businessNumber,
	@NotNull Long certificateFileId
) {
}
