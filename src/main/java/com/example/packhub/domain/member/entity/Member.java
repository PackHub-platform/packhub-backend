package com.example.packhub.domain.member.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long memberId;

	/** 로그인 ID */
	@Column(nullable = false, length = 255)
	private String email;

	/** BCrypt 해시. 원문 비밀번호는 저장하지 않는다 */
	@Column(nullable = false, length = 255)
	private String password;

	@Column(length = 50)
	private String name;

	/** 하이픈 없는 휴대폰 번호 */
	@Column(nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MemberType memberType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MemberStatus status;

	private LocalDateTime emailVerifiedAt;

	private LocalDateTime phoneVerifiedAt;

	private LocalDateTime lastLoginAt;

	private LocalDateTime withdrawnAt;

	/**
	 * 가입. 권한은 항상 USER로 시작하고 관리자는 DB에서 직접 지정한다.
	 *
	 * @param encodedPassword PasswordEncoder로 암호화한 비밀번호
	 */
	public static Member signUp(String email, String encodedPassword, String name, String phone, MemberType memberType) {
		Member member = new Member();
		member.email = email;
		member.password = encodedPassword;
		member.name = name;
		member.phone = phone;
		member.memberType = memberType;
		member.role = Role.USER;
		member.status = MemberStatus.ACTIVE;
		return member;
	}

	public void recordLogin(LocalDateTime loggedInAt) {
		this.lastLoginAt = loggedInAt;
	}
}
