package com.example.packhub.domain.company.dto;

import com.example.packhub.domain.file.entity.File;

/**
 * 업체에 연결된 파일 하나. url은 유효 시간이 있는 조회용 URL이라 저장해 두고 다시 쓰지 않는다.
 * fileId는 수정할 때 그대로 다시 보내는 값이고, contentType으로 이미지인지 PDF인지 구분한다.
 */
public record CompanyFileResponse(
	Long fileId,
	String url,
	String contentType,
	String originalName
) {

	public static CompanyFileResponse of(File file, String url) {
		return new CompanyFileResponse(file.getFileId(), url, file.getContentType(), file.getOriginalName());
	}
}
