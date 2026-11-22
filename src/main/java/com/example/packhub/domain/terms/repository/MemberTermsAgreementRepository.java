package com.example.packhub.domain.terms.repository;

import com.example.packhub.domain.terms.entity.MemberTermsAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

	List<MemberTermsAgreement> findByMemberId(Long memberId);
}
