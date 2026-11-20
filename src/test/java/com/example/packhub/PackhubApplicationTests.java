package com.example.packhub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PackhubApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void Flyway_마이그레이션으로_ERD의_테이블이_모두_생성된다() {
		Integer tableCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.tables "
				+ "WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'",
			Integer.class);

		assertThat(tableCount).isEqualTo(35); // ERD 34개 + RefreshToken
	}
}
