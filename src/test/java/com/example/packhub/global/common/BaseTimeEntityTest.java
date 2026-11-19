package com.example.packhub.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.packhub.global.config.JpaAuditingConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BaseTimeEntityTest {

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
	void 수정하면_수정_시각만_바뀐다() {
		AuditingTestEntity entity = new AuditingTestEntity();
		em.persist(entity);
		em.flush();
		LocalDateTime createdAt = entity.getCreatedAt();
		LocalDateTime updatedAt = entity.getUpdatedAt();

		entity.name = "changed";
		em.flush();

		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
	}

	@Entity
	static class AuditingTestEntity extends BaseTimeEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;
	}
}
