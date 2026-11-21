package com.example.packhub.domain.file.dto;

/** url은 유효 시간이 있는 조회용 URL(presigned GET)이라 저장해 두고 다시 쓰지 않는다 */
public record FileResponse(
	Long fileId,
	String url
) {
}
