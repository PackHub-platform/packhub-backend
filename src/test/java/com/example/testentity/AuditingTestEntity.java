package com.example.testentity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * BaseTimeEntityTest 전용 엔티티.
 * 앱 패키지(com.example.packhub) 밖에 두어야 전체 컨텍스트 테스트의 엔티티 스캔·스키마 검증에 잡히지 않는다.
 */
@Entity
public class AuditingTestEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	public void changeName(String name) {
		this.name = name;
	}
}
