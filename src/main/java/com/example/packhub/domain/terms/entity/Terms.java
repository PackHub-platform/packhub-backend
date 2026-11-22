package com.example.packhub.domain.terms.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약관 한 버전 (종류 + 버전 = 1행). 내용이 바뀌면 행을 고치지 않고 새 버전 행을 추가한다.
 * 회원이 어느 버전에 동의했는지 남아야 하기 때문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long termsId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TermsType termsType;

	@Column(nullable = false, length = 20)
	private String version;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private boolean isRequired;

	/** 이 시각부터 시행. 지금보다 뒤면 아직 시행 전이다 */
	@Column(nullable = false)
	private LocalDateTime effectiveAt;
}
