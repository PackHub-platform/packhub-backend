package com.example.packhub.domain.terms.repository;

import com.example.packhub.domain.terms.entity.Terms;
import com.example.packhub.domain.terms.entity.TermsType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {

	/**
	 * 시행 중인(시행일이 지난) 약관을 시행일 최신순으로. 종류별 최신 버전은 서비스에서 고른다.
	 * 시행일이 같으면 나중에 넣은 것(termsId가 큰 것)을 앞에 둔다. 목록 조회와 가입 검사가 항상 같은 버전을 고르게 하기 위해서다
	 */
	List<Terms> findByTermsTypeInAndEffectiveAtLessThanEqualOrderByEffectiveAtDescTermsIdDesc(Collection<TermsType> termsTypes,
		LocalDateTime now);

	Optional<Terms> findByTermsTypeAndVersion(TermsType termsType, String version);
}
