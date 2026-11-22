package com.example.packhub.domain.terms.repository;

import com.example.packhub.domain.terms.entity.Terms;
import com.example.packhub.domain.terms.entity.TermsType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {

	/** 시행 중인(시행일이 지난) 약관을 시행일 최신순으로. 종류별 최신 버전은 서비스에서 고른다 */
	List<Terms> findByTermsTypeInAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(Collection<TermsType> termsTypes,
		LocalDateTime now);

	Optional<Terms> findByTermsTypeAndVersion(TermsType termsType, String version);
}
