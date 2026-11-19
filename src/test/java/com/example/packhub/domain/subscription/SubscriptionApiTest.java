package com.example.packhub.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.subscription.entity.Payment;
import com.example.packhub.domain.subscription.entity.PaymentStatus;
import com.example.packhub.domain.subscription.repository.PartnerConsultationRepository;
import com.example.packhub.domain.subscription.repository.PaymentRepository;
import com.example.packhub.domain.subscription.repository.SubscriptionRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 결제 요청은 재시도를 위해 트랜잭션 밖에서 저장되므로(테스트 롤백 불가) 테스트마다 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SubscriptionApiTest {

	private static final long BASIC = 1L;
	private static final long PARTNER = 3L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private PartnerConsultationRepository partnerConsultationRepository;

	@AfterEach
	void cleanUp() {
		paymentRepository.deleteAllInBatch();
		subscriptionRepository.deleteAllInBatch();
		partnerConsultationRepository.deleteAllInBatch();
	}

	@Test
	void 플랜_목록은_기초_데이터_3개를_순서대로_반환한다() throws Exception {
		mockMvc.perform(get("/api/plans"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(3))
			.andExpect(jsonPath("$.data[0].code").value("BASIC"))
			.andExpect(jsonPath("$.data[0].monthlyPrice").value(29000))
			.andExpect(jsonPath("$.data[0].isConsultOnly").value(false))
			.andExpect(jsonPath("$.data[2].code").value("PARTNER"))
			.andExpect(jsonPath("$.data[2].isConsultOnly").value(true));
	}

	@Test
	void 결제_요청은_결제_대기_주문을_저장하고_금액과_주문번호를_반환한다() throws Exception {
		String orderNo = orderNo(order(1L, BASIC, "YEARLY", true)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.planName").value("PackHub Basic"))
			.andExpect(jsonPath("$.data.billingCycle").value("YEARLY"))
			.andExpect(jsonPath("$.data.amount").value(290000)));

		Payment saved = findPayment(orderNo);
		assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(saved.getMemberId()).isEqualTo(1L);
		assertThat(saved.getAmount()).isEqualTo(290000L);
		assertThat(saved.getSubscriptionId()).isNull();
	}

	@Test
	void 주문번호는_오늘_날짜와_000001부터_이어지는_번호다() throws Exception {
		String today = "PH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";

		assertThat(orderNo(order(1L, BASIC, "MONTHLY", true))).isEqualTo(today + "000001");
		assertThat(orderNo(order(2L, BASIC, "MONTHLY", true))).isEqualTo(today + "000002");
	}

	@Test
	void 파트너_플랜은_결제할_수_없다() throws Exception {
		order(1L, PARTNER, "MONTHLY", true)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("CONSULT_ONLY_PLAN"));
	}

	@Test
	void 필수_동의를_하지_않으면_결제_요청이_실패한다() throws Exception {
		order(1L, BASIC, "MONTHLY", false)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 없는_플랜은_404() throws Exception {
		order(1L, 999L, "MONTHLY", true)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PLAN_NOT_FOUND"));
	}

	@Test
	void 회원_ID_헤더가_없으면_400() throws Exception {
		mockMvc.perform(get("/api/subscriptions/me"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 이용권이_없으면_현재_이용권_조회의_data가_없다() throws Exception {
		mockMvc.perform(get("/api/subscriptions/me").header("X-Member-Id", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 결제_승인_후_현재_이용권에_ACTIVE로_조회된다() throws Exception {
		LocalDate today = LocalDate.now();
		String orderNo = orderNo(order(1L, BASIC, "MONTHLY", true));

		confirm(1L, orderNo)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.orderNo").value(orderNo))
			.andExpect(jsonPath("$.data.amount").value(29000))
			.andExpect(jsonPath("$.data.methodSummary").value("테스트 결제"))
			.andExpect(jsonPath("$.data.periodStart").value(today.toString()))
			.andExpect(jsonPath("$.data.periodEnd").value(today.plusMonths(1).minusDays(1).toString()));

		Payment paid = findPayment(orderNo);
		assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(paid.getSubscriptionId()).isNotNull();

		mockMvc.perform(get("/api/subscriptions/me").header("X-Member-Id", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.plan.code").value("BASIC"))
			.andExpect(jsonPath("$.data.price").value(29000))
			.andExpect(jsonPath("$.data.nextBillingAmount").value(29000))
			.andExpect(jsonPath("$.data.nextBillingDate").value(today.plusMonths(1).toString()))
			.andExpect(jsonPath("$.data.autoRenew").value(true))
			.andExpect(jsonPath("$.data.paymentMethod").isEmpty())
			.andExpect(jsonPath("$.data.recentPayments.length()").value(1))
			.andExpect(jsonPath("$.data.recentPayments[0].status").value("PAID"));
	}

	@Test
	void 이용권이_있으면_다시_결제할_수_없다() throws Exception {
		String first = orderNo(order(1L, BASIC, "MONTHLY", true));
		String second = orderNo(order(1L, BASIC, "MONTHLY", true));
		confirm(1L, first).andExpect(status().isOk());

		order(1L, BASIC, "MONTHLY", true)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_ALREADY_ACTIVE"));
		confirm(1L, second)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_ALREADY_ACTIVE"));
	}

	@Test
	void 이미_처리된_주문은_다시_승인할_수_없다() throws Exception {
		String orderNo = orderNo(order(1L, BASIC, "MONTHLY", true));
		confirm(1L, orderNo).andExpect(status().isOk());

		confirm(1L, orderNo)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("ORDER_ALREADY_PROCESSED"));
	}

	@Test
	void 다른_회원의_주문과_없는_주문은_404() throws Exception {
		String orderNo = orderNo(order(1L, BASIC, "MONTHLY", true));

		confirm(2L, orderNo)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
		confirm(1L, "PH-20000101-000001")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
	}

	@Test
	void 파트너_상담을_신청하면_201과_ID를_반환한다() throws Exception {
		mockMvc.perform(post("/api/partner-consultations")
				.header("X-Member-Id", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"companyName":"팩허브포장","managerName":"홍길동","phone":"010-1234-5678",
					 "email":"contact@example.com","content":"맞춤 견적 상담 원합니다.","privacyAgreed":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.partnerConsultationId").isNumber());
	}

	@Test
	void 파트너_상담은_개인정보_동의와_이메일_형식이_필요하다() throws Exception {
		mockMvc.perform(post("/api/partner-consultations")
				.header("X-Member-Id", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"companyName":"팩허브포장","managerName":"홍길동","phone":"010-1234-5678",
					 "email":"not-an-email","content":"상담","privacyAgreed":false}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors.length()").value(2));
	}

	private ResultActions order(long memberId, long planId, String billingCycle, boolean agreed) throws Exception {
		return mockMvc.perform(post("/api/subscriptions")
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"planId":%d,"billingCycle":"%s","agreePaymentTerms":%b,"agreeRecurringPayment":%b}
				""".formatted(planId, billingCycle, agreed, agreed)));
	}

	private ResultActions confirm(long memberId, String orderNo) throws Exception {
		return mockMvc.perform(post("/api/subscriptions/confirm")
			.header("X-Member-Id", memberId)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"orderNo":"%s"}
				""".formatted(orderNo)));
	}

	private Payment findPayment(String orderNo) {
		return paymentRepository.findAll().stream()
			.filter(payment -> payment.getOrderNo().equals(orderNo))
			.findFirst()
			.orElseThrow();
	}

	private String orderNo(ResultActions orderResult) throws Exception {
		return JsonPath.read(orderResult.andReturn().getResponse().getContentAsString(), "$.data.orderNo");
	}
}
