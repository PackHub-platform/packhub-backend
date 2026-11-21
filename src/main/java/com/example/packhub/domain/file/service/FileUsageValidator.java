package com.example.packhub.domain.file.service;

import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 다른 기능(업체 등록 등)이 요청으로 받은 fileId를 저장하기 전에 호출한다.
 * 남의 파일이나 다른 용도로 올린 파일을 끼워 넣지 못하게 막는다.
 */
@Component
@RequiredArgsConstructor
public class FileUsageValidator {

	private final FileRepository fileRepository;

	/**
	 * 없거나 삭제된 파일이면 404 FILE_NOT_FOUND, 남이 올린 파일이면 403 FILE_ACCESS_DENIED,
	 * 다른 용도로 올린 파일이면 400 FILE_PURPOSE_MISMATCH. 용도는 저장 키의 첫 칸(폴더)으로 판단한다.
	 */
	public void validate(Long memberId, FilePurpose purpose, Collection<Long> fileIds) {
		Set<Long> ids = new HashSet<>(fileIds);
		if (ids.isEmpty()) {
			return;
		}
		List<File> files = fileRepository.findAllById(ids);
		if (files.size() != ids.size() || files.stream().anyMatch(file -> file.getDeletedAt() != null)) {
			throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
		}
		for (File file : files) {
			if (!file.getUploaderId().equals(memberId)) {
				throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
			}
			if (!file.getStorageKey().startsWith(purpose.getFolder() + "/")) {
				throw new BusinessException(ErrorCode.FILE_PURPOSE_MISMATCH);
			}
		}
	}
}
