package com.example.packhub.domain.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.packhub.TestcontainersConfiguration;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.entity.CompanyCategory;
import com.example.packhub.domain.company.repository.CompanyCategoryRepository;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.favorite.entity.Favorite;
import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import com.example.packhub.domain.favorite.repository.FavoriteRepository;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.global.security.jwt.JwtProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 업체 관심 등록·해제, 관심 목록, 업체 목록·상세의 favorited 표시.
 * 요청이 실제로 커밋되므로(테스트 롤백 불가) 테스트마다 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FavoriteApiTest {

	/** 기초 데이터: 대분류 "연포장·파우치" 아래의 소분류 "삼방봉투" */
	private static final long SUB_CATEGORY = 101L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FavoriteRepository favoriteRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private CompanyCategoryRepository companyCategoryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** 관심을 저장하는 일반회원 */
	private Long general;
	/** 다른 일반회원 */
	private Long other;
	private Long companyA;
	private Long companyB;

	@BeforeEach
	void setUp() {
		general = saveMember("fav-general@test.com", "010-9300-0001", MemberType.GENERAL);
		other = saveMember("fav-other@test.com", "010-9300-0002", MemberType.GENERAL);
		companyA = saveCompany("fav-owner-a@test.com", "010-9300-0003", "가나 포장", "연포장 전문 업체입니다.");
		companyB = saveCompany("fav-owner-b@test.com", "010-9300-0004", "다라 박스", "골판지 박스를 만듭니다.");
	}

	@AfterEach
	void cleanUp() {
		favoriteRepository.deleteAllInBatch();
		companyCategoryRepository.deleteAllInBatch();
		companyRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
	}

	@Test
	void 업체를_관심으로_등록한다() throws Exception {
		add(general, companyA).andExpect(status().isOk());

		List<Favorite> favorites = favoriteRepository.findAll();
		assertThat(favorites).hasSize(1);
		assertThat(favorites.get(0).getMemberId()).isEqualTo(general);
		assertThat(favorites.get(0).getTargetType()).isEqualTo(FavoriteTargetType.COMPANY);
		assertThat(favorites.get(0).getCompanyId()).isEqualTo(companyA);
		assertThat(favorites.get(0).getPostId()).isNull();
	}

	@Test
	void 이미_저장한_업체를_다시_등록해도_성공이고_한_번만_저장된다() throws Exception {
		add(general, companyA).andExpect(status().isOk());
		add(general, companyA).andExpect(status().isOk());

		assertThat(favoriteRepository.count()).isEqualTo(1);
	}

	@Test
	void 같은_등록_요청이_동시에_와도_둘_다_성공이고_한_번만_저장된다() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> request = () -> {
			start.await();
			return add(general, companyA).andReturn().getResponse().getStatus();
		};
		try {
			Future<Integer> first = executor.submit(request);
			Future<Integer> second = executor.submit(request);
			start.countDown();

			assertThat(List.of(first.get(), second.get())).containsExactly(200, 200);
		} finally {
			executor.shutdownNow();
		}
		assertThat(favoriteRepository.count()).isEqualTo(1);
	}

	@Test
	void 코드의_확인을_거치지_않아도_DB가_같은_업체의_중복_저장을_막는다() {
		favoriteRepository.save(Favorite.ofCompany(general, companyA));

		assertThatThrownBy(() -> jdbcTemplate.update(
			"INSERT INTO `Favorite` (`memberId`, `targetType`, `companyId`) VALUES (?, 'COMPANY', ?)",
			general, companyA))
			.isInstanceOf(DuplicateKeyException.class)
			.hasMessageContaining("UK_FAVORITE_MEMBER_COMPANY");
	}

	@Test
	void 여러_업체와_여러_회원이_각자_저장할_수_있다() throws Exception {
		add(general, companyA).andExpect(status().isOk());
		add(general, companyB).andExpect(status().isOk());
		add(other, companyA).andExpect(status().isOk());

		assertThat(favoriteRepository.count()).isEqualTo(3);
	}

	@Test
	void 기업회원도_관심을_등록할_수_있고_자기_업체도_저장할_수_있다() throws Exception {
		Long owner = companyRepository.findById(companyA).orElseThrow().getMemberId();

		add(owner, companyA).andExpect(status().isOk());
		add(owner, companyB).andExpect(status().isOk());

		assertThat(favoriteRepository.count()).isEqualTo(2);
	}

	@Test
	void 없는_업체나_숨김_삭제된_업체는_등록할_수_없다() throws Exception {
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", companyA);
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `companyId` = ?", companyB);

		for (long companyId : new long[] {999999L, companyA, companyB}) {
			add(general, companyId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("COMPANY_NOT_FOUND"));
		}
		assertThat(favoriteRepository.count()).isZero();
	}

	@Test
	void 관심을_해제하면_그_회원의_그_업체만_지워진다() throws Exception {
		add(general, companyA);
		add(general, companyB);
		add(other, companyA);

		remove(general, companyA).andExpect(status().isOk());

		assertThat(favoriteRepository.existsByMemberIdAndCompanyId(general, companyA)).isFalse();
		assertThat(favoriteRepository.existsByMemberIdAndCompanyId(general, companyB)).isTrue();
		assertThat(favoriteRepository.existsByMemberIdAndCompanyId(other, companyA)).isTrue();
	}

	@Test
	void 저장하지_않은_업체를_해제해도_성공이다() throws Exception {
		remove(general, companyA).andExpect(status().isOk());
		remove(general, 999999L).andExpect(status().isOk());
	}

	@Test
	void 해제한_뒤에_다시_등록할_수_있다() throws Exception {
		add(general, companyA);
		remove(general, companyA);

		add(general, companyA).andExpect(status().isOk());

		assertThat(favoriteRepository.count()).isEqualTo(1);
	}

	@Test
	void 토큰이_없으면_등록_해제_목록_모두_401() throws Exception {
		mockMvc.perform(put("/api/v1/bookmarks/companies/{companyId}", companyA))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/bookmarks/companies/{companyId}", companyA))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/bookmarks"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 관심_목록은_내가_저장한_업체를_최근에_저장한_순서로_반환한다() throws Exception {
		add(general, companyA);
		add(general, companyB);
		add(other, companyA);
		// 저장한 시각이 같은 초에 들어가도 순서가 분명하도록 먼저 저장한 쪽을 하루 전으로 옮긴다
		jdbcTemplate.update(
			"UPDATE `Favorite` SET `createdAt` = `createdAt` - INTERVAL 1 DAY WHERE `memberId` = ? AND `companyId` = ?",
			general, companyA);

		list(general)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.content[0].targetType").value("COMPANY"))
			.andExpect(jsonPath("$.data.content[0].targetId").value(companyB))
			.andExpect(jsonPath("$.data.content[0].title").value("다라 박스"))
			.andExpect(jsonPath("$.data.content[0].content").value("골판지 박스를 만듭니다."))
			.andExpect(jsonPath("$.data.content[0].date").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.data.content[1].targetId").value(companyA))
			.andExpect(jsonPath("$.data.content[1].date").value(LocalDate.now().minusDays(1).toString()))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(20))
			.andExpect(jsonPath("$.data.totalElements").value(2))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	void 저장한_것이_없으면_관심_목록은_비어_있다() throws Exception {
		list(general)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 저장한_뒤에_숨김_삭제된_업체는_관심_목록에서_빠진다() throws Exception {
		Long companyC = saveCompany("fav-owner-c@test.com", "010-9300-0005", "마바 필름", "필름을 만듭니다.");
		add(general, companyA);
		add(general, companyB);
		add(general, companyC);
		jdbcTemplate.update("UPDATE `Company` SET `visibility` = 'HIDDEN' WHERE `companyId` = ?", companyA);
		jdbcTemplate.update("UPDATE `Company` SET `deletedAt` = NOW() WHERE `companyId` = ?", companyB);

		list(general)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].targetId").value(companyC))
			.andExpect(jsonPath("$.data.totalElements").value(1));
		assertThat(favoriteRepository.count()).isEqualTo(3);
	}

	@Test
	void 관심_목록은_종류로_고를_수_있고_게시글은_아직_없어서_비어_있다() throws Exception {
		add(general, companyA);

		mockMvc.perform(get("/api/v1/bookmarks").param("targetType", "COMPANY")
				.header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1));

		mockMvc.perform(get("/api/v1/bookmarks").param("targetType", "POST")
				.header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0))
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 관심_목록을_페이지로_나눠서_반환한다() throws Exception {
		add(general, companyA);
		add(general, companyB);

		mockMvc.perform(get("/api/v1/bookmarks").param("size", "1").param("page", "2")
				.header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.page").value(2))
			.andExpect(jsonPath("$.data.size").value(1))
			.andExpect(jsonPath("$.data.totalElements").value(2))
			.andExpect(jsonPath("$.data.totalPages").value(2));
	}

	@Test
	void 관심_목록_조건이_잘못되면_400() throws Exception {
		for (String[] param : new String[][] {{"page", "0"}, {"size", "51"}, {"targetType", "MEMBER"}}) {
			mockMvc.perform(get("/api/v1/bookmarks").param(param[0], param[1])
					.header(HttpHeaders.AUTHORIZATION, bearer(general)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		}
	}

	@Test
	void 업체_목록은_로그인한_회원이_저장한_업체만_favorited가_true다() throws Exception {
		add(general, companyA);
		add(other, companyB);

		// 최신순이라 나중에 만든 companyB가 먼저 나온다
		mockMvc.perform(get("/api/v1/companies").header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].companyId").value(contains(companyB.intValue(), companyA.intValue())))
			.andExpect(jsonPath("$.data.content[*].favorited").value(contains(false, true)));
	}

	@Test
	void 로그인하지_않으면_업체_목록과_상세의_favorited는_false다() throws Exception {
		add(general, companyA);

		mockMvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[*].favorited").value(contains(false, false)));
		mockMvc.perform(get("/api/v1/companies/{companyId}", companyA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(false));
	}

	@Test
	void 업체_상세는_로그인한_회원이_저장했으면_favorited가_true이고_해제하면_false다() throws Exception {
		add(general, companyA);

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyA)
				.header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(true));
		mockMvc.perform(get("/api/v1/companies/{companyId}", companyA)
				.header(HttpHeaders.AUTHORIZATION, bearer(other)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(false));

		remove(general, companyA);

		mockMvc.perform(get("/api/v1/companies/{companyId}", companyA)
				.header(HttpHeaders.AUTHORIZATION, bearer(general)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.favorited").value(false));
	}

	private ResultActions add(long memberId, long companyId) throws Exception {
		return mockMvc.perform(put("/api/v1/bookmarks/companies/{companyId}", companyId)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId)));
	}

	private ResultActions remove(long memberId, long companyId) throws Exception {
		return mockMvc.perform(delete("/api/v1/bookmarks/companies/{companyId}", companyId)
			.header(HttpHeaders.AUTHORIZATION, bearer(memberId)));
	}

	private ResultActions list(long memberId) throws Exception {
		return mockMvc.perform(get("/api/v1/bookmarks").header(HttpHeaders.AUTHORIZATION, bearer(memberId)));
	}

	private String bearer(long memberId) {
		return "Bearer " + jwtProvider.createAccessToken(memberId, "USER");
	}

	private Long saveMember(String email, String phone, MemberType memberType) {
		return memberRepository.save(Member.signUp(email, "{noop}password", "테스트", phone, memberType)).getMemberId();
	}

	/** 새 기업회원을 만들고 그 회원의 업체를 API를 거치지 않고 바로 저장한다 */
	private Long saveCompany(String ownerEmail, String ownerPhone, String name, String description) {
		Long owner = saveMember(ownerEmail, ownerPhone, MemberType.COMPANY);
		Long companyId = companyRepository.save(Company.builder()
			.memberId(owner)
			.name(name)
			.industry("연포장")
			.products("파우치")
			.businessHours("09:00 - 18:00")
			.contactPhone("02-1234-5678")
			.address("경기 안산시 단원구 1")
			.representativeName("홍길동")
			.foundedDate(LocalDate.of(2015, 3, 2))
			.employeeCount(12)
			.description(description)
			.build()).getCompanyId();
		companyCategoryRepository.save(CompanyCategory.of(companyId, SUB_CATEGORY));
		return companyId;
	}
}
