package com.example.packhub.domain.subscription.service;

import com.example.packhub.domain.member.service.CompanyMemberValidator;
import com.example.packhub.domain.subscription.dto.ConfirmRequest;
import com.example.packhub.domain.subscription.dto.ConfirmResponse;
import com.example.packhub.domain.subscription.dto.MySubscriptionResponse;
import com.example.packhub.domain.subscription.dto.OrderRequest;
import com.example.packhub.domain.subscription.dto.OrderResponse;
import com.example.packhub.domain.subscription.dto.PlanResponse;
import com.example.packhub.domain.subscription.entity.Payment;
import com.example.packhub.domain.subscription.entity.PaymentStatus;
import com.example.packhub.domain.subscription.entity.Plan;
import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.entity.SubscriptionStatus;
import com.example.packhub.domain.subscription.repository.PaymentRepository;
import com.example.packhub.domain.subscription.repository.PlanRepository;
import com.example.packhub.domain.subscription.repository.SubscriptionRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용권·결제 (중간 범위: PG 연동 없이 화면용).
 * 결제 요청은 Payment를 PENDING 주문으로 저장하고, 결제 승인은 항상 성공으로 처리해 Subscription을 시작한다.
 * 결제 요청·승인은 기업회원만 할 수 있다. 현재 이용권 조회는 누구나 할 수 있고, 일반회원은 이용권이 없어 빈 응답이다.
 * TODO(사업자 인증 1-4 머지 후): 결제 요청·승인에 사업자 인증 완료 체크(BUSINESS_NOT_VERIFIED) 추가.
 * TODO(기말): PG 승인·금액 검증·실패 처리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

	/** 아직 이용 기간이 남아 있는 상태. 이 상태의 이용권이 있으면 새로 결제할 수 없다. */
	private static final List<SubscriptionStatus> IN_USE = List.of(SubscriptionStatus.ACTIVE,
		SubscriptionStatus.CANCEL_SCHEDULED);
	private static final String MOCK_METHOD_SUMMARY = "테스트 결제";
	private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final int MAX_ORDER_NO_ATTEMPTS = 3;

	private final PlanRepository planRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PaymentRepository paymentRepository;
	private final CompanyMemberValidator companyMemberValidator;

	public List<PlanResponse> getPlans() {
		return planRepository.findByIsActiveTrueOrderByPlanIdAsc().stream()
			.map(PlanResponse::from)
			.toList();
	}

	/**
	 * 결제 대기 주문을 저장한다. 동시에 같은 주문번호가 발급되면 주문번호 UNIQUE 제약에 걸리므로 다시 시도한다.
	 * 시도마다 저장이 따로 커밋되도록 이 메서드는 트랜잭션 없이 실행한다.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public OrderResponse createOrder(Long memberId, OrderRequest request) {
		companyMemberValidator.validate(memberId);
		Plan plan = getPayablePlan(request.planId());
		validateNoSubscriptionInUse(memberId);

		for (int attempt = 1; ; attempt++) {
			try {
				Payment order = paymentRepository.saveAndFlush(
					Payment.pending(nextOrderNo(), memberId, plan, request.billingCycle()));
				return OrderResponse.from(order);
			} catch (DataIntegrityViolationException e) {
				if (attempt == MAX_ORDER_NO_ATTEMPTS) {
					throw e;
				}
			}
		}
	}

	@Transactional
	public ConfirmResponse confirm(Long memberId, ConfirmRequest request) {
		companyMemberValidator.validate(memberId);
		Payment order = paymentRepository.findForUpdateByOrderNo(request.orderNo())
			.filter(payment -> payment.getMemberId().equals(memberId))
			.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
		if (!order.isPending()) {
			throw new BusinessException(ErrorCode.ORDER_ALREADY_PROCESSED);
		}
		validateNoSubscriptionInUse(memberId);

		Subscription subscription = subscriptionRepository.save(Subscription.startPaid(
			memberId, order.getPlanId(), order.getBillingCycle(), LocalDate.now()));
		order.complete(subscription, MOCK_METHOD_SUMMARY, LocalDateTime.now());
		return ConfirmResponse.from(order);
	}

	/** 이용 중인 이용권이 없으면 null을 돌려준다. 최근 결제 내역에는 결제 완료(PAID) 건만 보여 준다. */
	public MySubscriptionResponse getMySubscription(Long memberId) {
		return subscriptionRepository.findFirstByMemberIdAndStatusIn(memberId, IN_USE)
			.map(subscription -> MySubscriptionResponse.of(
				subscription,
				planRepository.findById(subscription.getPlanId()).orElseThrow(),
				paymentRepository.findTop5ByMemberIdAndStatusOrderByPaymentIdDesc(memberId, PaymentStatus.PAID)))
			.orElse(null);
	}

	/** 이용 기간이 남은 이용권이 있는지. 업체 등록처럼 이용권이 있어야 쓸 수 있는 기능에서 확인한다 */
	public boolean hasSubscriptionInUse(Long memberId) {
		return subscriptionRepository.existsByMemberIdAndStatusIn(memberId, IN_USE);
	}

	private Plan getPayablePlan(Long planId) {
		Plan plan = planRepository.findById(planId)
			.filter(Plan::isActive)
			.orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
		if (plan.isConsultOnly()) {
			throw new BusinessException(ErrorCode.CONSULT_ONLY_PLAN);
		}
		return plan;
	}

	private void validateNoSubscriptionInUse(Long memberId) {
		if (hasSubscriptionInUse(memberId)) {
			throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
		}
	}

	/** 주문번호 PH-YYYYMMDD-NNNNNN. 뒤 6자리는 오늘 마지막 번호 + 1이고, 날짜가 바뀌면 000001부터 다시 시작한다. */
	private String nextOrderNo() {
		String prefix = "PH-" + LocalDate.now().format(ORDER_DATE) + "-";
		int last = paymentRepository.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
			.map(payment -> Integer.parseInt(payment.getOrderNo().substring(prefix.length())))
			.orElse(0);
		return prefix + "%06d".formatted(last + 1);
	}
}
