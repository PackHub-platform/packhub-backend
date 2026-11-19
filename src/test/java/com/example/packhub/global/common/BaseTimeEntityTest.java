package com.example.packhub.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.packhub.global.config.JpaAuditingConfig;
import com.example.testentity.AuditingTestEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

// 테스트 전용 엔티티를 쓰므로 Flyway 스키마 대신 내장 H2에 Hibernate가 테이블을 만든다
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(JpaAuditingConfig.class)
class BaseTimeEntityTest {

	@TestConfiguration
	@EntityScan(basePackageClasses = AuditingTestEntity.class)
	static class TestEntityScanConfig {
	}

	@Autowired
	private EntityManager em;

	@Test
	void 저장하면_생성_수정_시각이_채워진다() {
		AuditingTestEntity entity = new AuditingTestEntity();

		em.persist(entity);
		em.flush();

		assertThat(entity.getCreatedAt()).isNotNull();
		assertThat(entity.getUpdatedAt()).isNotNull();
	}

	@Test
	void 수정하면_수정_시각만_바뀐다() throws InterruptedException {
		AuditingTestEntity entity = new AuditingTestEntity();
		em.persist(entity);
		em.flush();
		LocalDateTime createdAt = entity.getCreatedAt();
		LocalDateTime updatedAt = entity.getUpdatedAt();

		Thread.sleep(10); // 저장과 수정 시각이 같게 찍히지 않도록 잠깐 기다린다
		entity.changeName("changed");
		em.flush();

		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getUpdatedAt()).isAfter(updatedAt);
	}
}
