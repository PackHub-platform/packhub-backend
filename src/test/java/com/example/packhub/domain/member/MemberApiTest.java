package com.example.packhub.domain.member;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.business.entity.BusinessVerification;
import com.example.packhub.domain.business.repository.BusinessVerificationRepository;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.subscription.entity.BillingCycle;
import com.example.packhub.domain.subscription.entity.Subscription;
import com.example.packhub.domain.subscription.repository.SubscriptionRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** 내 정보·권한 조회 (16-1) */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MemberApiTest {

	private static final long BASIC_PLAN = 1L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private BusinessVerificationRepository businessVerificationRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@AfterEach
	void cleanUp() {
		businessVerificationRepository.deleteAllInBatch();
		subscriptionRepository.deleteAllInBatch();
		companyRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 가입만_한_일반회원은_업체_이용권_사업자_인증이_모두_없다() throws Exception {
		Long general = saveMember("me-general@test.com", "01096100001", MemberType.GENERAL);

		mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.memberId").value(general))
			.andExpect(jsonPath("$.data.email").value("me-general@test.com"))
			.andExpect(jsonPath("$.data.name").value("김팩허"))
			.andExpect(jsonPath("$.data.phone").value("01096100001"))
			.andExpect(jsonPath("$.data.memberType").value("GENERAL"))
			.andExpect(jsonPath("$.data.role").value("USER"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andExpect(jsonPath("$.data.businessVerificationStatus").value("NONE"))
			.andExpect(jsonPath("$.data.companyId").value(nullValue()))
			.andExpect(jsonPath("$.data.hasActiveSubscription").value(false));
	}

	@Test
	void 인증_업체_이용권을_모두_갖춘_기업회원은_그대로_나온다() throws Exception {
		Long company = saveMember("me-company@test.com", "01096100002", MemberType.COMPANY);
		businessVerificationRepository.save(BusinessVerification.approveNow(company, "팩허브 패키징", "김팩허",
			"123-45-67891", 1L, LocalDateTime.now()));
		Long companyId = companyRepository.save(Company.builder()
			.memberId(company)
			.name("팩허브 패키징")
			.industry("포장")
			.products("지퍼파우치")
			.businessHours("09:00 - 18:00")
			.contactPhone("02-1234-5678")
			.address("경기 파주시 1")
			.representativeName("김팩허")
			.foundedDate(LocalDate.of(2015, 3, 2))
			.employeeCount(12)
			.description("설명")
			.build()).getCompanyId();
		subscriptionRepository.save(Subscription.startPaid(company, BASIC_PLAN, BillingCycle.MONTHLY, LocalDate.now()));

		mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(company)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.memberType").value("COMPANY"))
			.andExpect(jsonPath("$.data.businessVerificationStatus").value("APPROVED"))
			.andExpect(jsonPath("$.data.companyId").value(companyId))
			.andExpect(jsonPath("$.data.hasActiveSubscription").value(true));
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/api/v1/members/me"))
			.andExpect(status().isUnauthorized());
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone, MemberType memberType) {
		return memberRepository.save(Member.signUp(email, "{noop}password", "김팩허", phone, memberType)).getMemberId();
	}
}
