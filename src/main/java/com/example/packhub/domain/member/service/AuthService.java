package com.example.packhub.domain.member.service;

import com.example.packhub.domain.member.dto.LoginRequest;
import com.example.packhub.domain.member.dto.LoginResponse;
import com.example.packhub.domain.member.dto.ReissueResponse;
import com.example.packhub.domain.member.dto.SignUpNextStep;
import com.example.packhub.domain.member.dto.SignUpRequest;
import com.example.packhub.domain.member.dto.SignUpResponse;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.entity.MemberStatus;
import com.example.packhub.domain.member.entity.MemberType;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import com.example.packhub.global.security.jwt.JwtProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		if (!request.password().equals(request.passwordConfirm())) {
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}
		// 대부분의 중복은 여기서 친절한 에러로 거른다
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
		}
		if (memberRepository.existsByPhone(request.phone())) {
			throw new BusinessException(ErrorCode.MEMBER_PHONE_DUPLICATED);
		}

		Member member = Member.signUp(request.email(), passwordEncoder.encode(request.password()), request.name(),
			request.phone(), request.memberType());
		saveNewMember(member);

		SignUpNextStep nextStep = member.getMemberType() == MemberType.COMPANY
			? SignUpNextStep.BUSINESS_VERIFICATION
			: SignUpNextStep.DONE;
		return new SignUpResponse(member.getMemberId(), member.getMemberType(), member.getEmail(), nextStep,
			createAccessToken(member), refreshTokenService.issue(member.getMemberId()), TOKEN_TYPE,
			jwtProvider.getAccessTokenValiditySeconds());
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		// 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 에러로 돌려준다 (가입된 이메일인지 알려 주지 않기 위해)
		Member member = memberRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		// 계정 상태는 비밀번호가 맞은 사람에게만 알려 준다
		checkActive(member);

		member.recordLogin(LocalDateTime.now());
		return new LoginResponse(createAccessToken(member), refreshTokenService.issue(member.getMemberId()), TOKEN_TYPE,
			jwtProvider.getAccessTokenValiditySeconds(), refreshTokenService.getValiditySeconds(),
			LoginResponse.MemberSummary.from(member));
	}

	/**
	 * 받은 Refresh Token을 폐기하고 새 Access·Refresh 한 쌍을 준다 (회전).
	 * 그사이 정지·탈퇴된 회원이면 새 토큰을 주지 않는다.
	 */
	@Transactional
	public ReissueResponse reissue(String refreshToken) {
		Long memberId = refreshTokenService.consume(refreshToken);
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		checkActive(member);
		return new ReissueResponse(createAccessToken(member), refreshTokenService.issue(memberId), TOKEN_TYPE,
			jwtProvider.getAccessTokenValiditySeconds(), refreshTokenService.getValiditySeconds());
	}

	/** 이 기기의 Refresh Token만 폐기한다. Access Token은 만료(최대 30분)까지 서버가 따로 막지 않는다 */
	@Transactional
	public void logout(Long memberId, String refreshToken) {
		refreshTokenService.revoke(memberId, refreshToken);
	}

	private void checkActive(Member member) {
		if (member.getStatus() == MemberStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
		}
		if (member.getStatus() == MemberStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
		}
	}

	/**
	 * 위 중복 검사와 저장 사이에 같은 이메일·번호로 다른 가입이 끼어들면 DB 유니크 제약이 거절한다.
	 * 그 거절을 500 대신 409로 바꾸기 위해 바로 flush해서 이 자리에서 예외를 받는다.
	 */
	private void saveNewMember(Member member) {
		try {
			memberRepository.saveAndFlush(member);
		} catch (DataIntegrityViolationException e) {
			String message = String.valueOf(e.getMostSpecificCause().getMessage());
			if (message.contains("UK_Member_email")) {
				throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
			}
			if (message.contains("UK_Member_phone")) {
				throw new BusinessException(ErrorCode.MEMBER_PHONE_DUPLICATED);
			}
			throw e;
		}
	}

	private String createAccessToken(Member member) {
		return jwtProvider.createAccessToken(member.getMemberId(), member.getRole().name());
	}
}
