package com.example.packhub.domain.file.service;

import com.example.packhub.domain.file.dto.FileRegisterRequest;
import com.example.packhub.domain.file.dto.FileResponse;
import com.example.packhub.domain.file.dto.PresignedUrlRequest;
import com.example.packhub.domain.file.dto.PresignedUrlResponse;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.repository.FileRepository;
import com.example.packhub.global.config.S3Properties;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * 파일 업로드 (S3 presigned URL).
 * 1단계 업로드 URL 발급 → 2단계 브라우저가 S3에 직접 PUT → 3단계 업로드 확인 후 File 저장.
 */
@Service
@RequiredArgsConstructor
public class FileService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;
	private final FileRepository fileRepository;

	/** 1단계: contentType과 크기를 서명에 넣어, 다른 형식·크기로 올리면 S3가 거부하게 한다 */
	public PresignedUrlResponse issueUploadUrl(Long memberId, PresignedUrlRequest request) {
		request.purpose().validate(request.contentType(), request.sizeBytes());
		String storageKey = StorageKey.create(request.purpose(), memberId, request.contentType());

		PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presign -> presign
			.signatureDuration(s3Properties.uploadUrlExpiry())
			.putObjectRequest(put -> put
				.bucket(s3Properties.bucket())
				.key(storageKey)
				.contentType(request.contentType())
				.contentLength(request.sizeBytes())));

		return new PresignedUrlResponse(presigned.url().toString(), storageKey,
			LocalDateTime.ofInstant(presigned.expiration(), ZoneId.systemDefault()));
	}

	/**
	 * 3단계: S3에 파일이 있는지 확인하고 File을 저장한다. 같은 저장 키로 다시 오면 처음 만든 File을 돌려준다.
	 * 동시에 같은 키가 들어오면 저장 키 UNIQUE 위반을 잡아 먼저 저장된 것을 찾아야 해서 트랜잭션 없이 실행한다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public FileResponse register(Long memberId, FileRegisterRequest request) {
		if (!StorageKey.isOwnedBy(request.storageKey(), memberId)) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
		}
		var existing = fileRepository.findByStorageKey(request.storageKey());
		if (existing.isPresent()) {
			return toResponse(existing.get());
		}

		HeadObjectResponse uploaded = headObject(request.storageKey());
		try {
			File file = fileRepository.saveAndFlush(File.register(memberId, request.originalName(),
				request.storageKey(), uploaded.contentType(), uploaded.contentLength()));
			return toResponse(file);
		} catch (DataIntegrityViolationException e) {
			return toResponse(fileRepository.findByStorageKey(request.storageKey()).orElseThrow(() -> e));
		}
	}

	/** 다른 기능(업체 상세 등)에서 이미지를 보여 줄 때 쓰는 조회 URL. 유효 시간은 packhub.s3.view-url-expiry */
	public String viewUrl(String storageKey) {
		return s3Presigner.presignGetObject(presign -> presign
				.signatureDuration(s3Properties.viewUrlExpiry())
				.getObjectRequest(get -> get.bucket(s3Properties.bucket()).key(storageKey)))
			.url().toString();
	}

	/** 다른 기능(업체 상세 등)이 저장해 둔 fileId로 파일을 찾는다. 삭제된 파일은 빼고 돌려준다 */
	public List<File> findFiles(Collection<Long> fileIds) {
		return fileRepository.findAllById(fileIds).stream()
			.filter(file -> file.getDeletedAt() == null)
			.toList();
	}

	private HeadObjectResponse headObject(String storageKey) {
		try {
			return s3Client.headObject(head -> head.bucket(s3Properties.bucket()).key(storageKey));
		} catch (S3Exception e) {
			if (e.statusCode() == HttpStatus.NOT_FOUND.value()) {
				throw new BusinessException(ErrorCode.FILE_NOT_UPLOADED);
			}
			throw e;
		}
	}

	private FileResponse toResponse(File file) {
		return new FileResponse(file.getFileId(), viewUrl(file.getStorageKey()));
	}
}
