package com.example.packhub.domain.favorite.service;

import com.example.packhub.domain.company.entity.CompanyVisibility;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.favorite.dto.FavoritePageResponse;
import com.example.packhub.domain.favorite.dto.FavoriteResponse;
import com.example.packhub.domain.favorite.dto.FavoriteSearchRequest;
import com.example.packhub.domain.favorite.entity.Favorite;
import com.example.packhub.domain.favorite.entity.FavoriteTargetType;
import com.example.packhub.domain.favorite.repository.FavoriteRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관심 등록·해제·목록. 로그인한 회원이면 누구나 쓴다.
 * 지금은 업체 관심만 있다. 게시글 관심은 Post가 생긴 뒤 추가한다.
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

	/** 같은 회원이 같은 업체를 두 번 저장하지 못하게 막는 DB 제약 이름 (V20261123_0130 마이그레이션) */
	private static final String MEMBER_COMPANY_UNIQUE = "UK_FAVORITE_MEMBER_COMPANY";

	private final FavoriteRepository favoriteRepository;
	private final CompanyRepository companyRepository;

	/**
	 * 이미 저장했으면 아무것도 하지 않는다. 없거나 숨김·삭제된 업체는 404 COMPANY_NOT_FOUND.
	 * 트랜잭션으로 묶지 않는다. 묶으면 중복 제약 오류를 잡아도 트랜잭션이 되돌리기로 표시되어 응답이 오류로 나간다.
	 */
	public void addCompany(Long memberId, Long companyId) {
		if (!companyRepository.existsByCompanyIdAndVisibilityAndDeletedAtIsNull(companyId, CompanyVisibility.VISIBLE)) {
			throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
		}
		if (favoriteRepository.existsByMemberIdAndCompanyId(memberId, companyId)) {
			return;
		}
		try {
			favoriteRepository.saveAndFlush(Favorite.ofCompany(memberId, companyId));
		} catch (DataIntegrityViolationException e) {
			// 동시에 들어온 같은 요청이 먼저 저장했다. 이미 저장된 상태이므로 성공으로 본다
			if (!String.valueOf(e.getMostSpecificCause().getMessage()).contains(MEMBER_COMPANY_UNIQUE)) {
				throw e;
			}
		}
	}

	/** 저장하지 않은 업체여도 성공으로 본다 */
	@Transactional
	public void removeCompany(Long memberId, Long companyId) {
		favoriteRepository.deleteCompany(memberId, companyId);
	}

	/** 최근에 저장한 순서. 게시글 관심은 아직 없어서 POST만 요청하면 빈 목록이다 */
	@Transactional(readOnly = true)
	public FavoritePageResponse getFavorites(Long memberId, FavoriteSearchRequest request) {
		Pageable pageable = PageRequest.of(request.page() - 1, request.size());
		if (request.targetType() == FavoriteTargetType.POST) {
			return FavoritePageResponse.of(Page.<FavoriteResponse>empty(pageable));
		}
		return FavoritePageResponse.of(
			favoriteRepository.findCompanies(memberId, CompanyVisibility.VISIBLE, pageable));
	}
}
