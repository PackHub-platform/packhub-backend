package com.example.packhub.domain.file.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * S3에 업로드가 끝난 파일. 실제 파일은 S3에 있고, 여기에는 저장 키와 정보만 둔다.
 * 클래스 이름이 java.io.File과 같으니 import에 주의한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fileId;

	@Column(nullable = false)
	private Long uploaderId;

	@Column(nullable = false, length = 255)
	private String originalName;

	@Column(nullable = false, length = 500, unique = true)
	private String storageKey;

	@Column(nullable = false, length = 100)
	private String contentType;

	@Column(nullable = false)
	private Long sizeBytes;

	private LocalDateTime deletedAt;

	/** 형식과 크기는 요청 값이 아니라 S3가 알려 준 실제 값을 넣는다 */
	public static File register(Long uploaderId, String originalName, String storageKey, String contentType,
		long sizeBytes) {
		File file = new File();
		file.uploaderId = uploaderId;
		file.originalName = originalName;
		file.storageKey = storageKey;
		file.contentType = contentType;
		file.sizeBytes = sizeBytes;
		return file;
	}
}
