package com.example.packhub.domain.subscription.service;

import com.example.packhub.domain.subscription.dto.PartnerConsultationRequest;
import com.example.packhub.domain.subscription.entity.PartnerConsultation;
import com.example.packhub.domain.subscription.repository.PartnerConsultationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerConsultationService {

	private final PartnerConsultationRepository partnerConsultationRepository;

	@Transactional
	public Long apply(Long memberId, PartnerConsultationRequest request) {
		PartnerConsultation consultation = PartnerConsultation.receive(memberId, request.companyName(),
			request.managerName(), request.phone(), request.email(), request.content(), LocalDateTime.now());
		return partnerConsultationRepository.save(consultation).getPartnerConsultationId();
	}
}
