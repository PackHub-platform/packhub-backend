package com.example.packhub.domain.member.repository;

import com.example.packhub.domain.member.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * 아직 폐기되지 않은 토큰만 폐기한다. 바뀐 행 수(0 또는 1)를 돌려준다.
	 * "읽고 → 확인하고 → 쓰기"로 나누면 같은 토큰으로 동시에 들어온 재발급이 둘 다 성공할 수 있어서,
	 * 확인과 폐기를 UPDATE 한 번으로 묶는다. 먼저 온 요청만 1을 받는다.
	 */
	@Modifying
	@Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.refreshTokenId = :id AND t.revokedAt IS NULL")
	int revokeIfActive(@Param("id") Long refreshTokenId, @Param("now") LocalDateTime now);
}
