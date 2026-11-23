package com.example.packhub.domain.favorite.entity;

import com.example.packhub.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 저장한 관심 대상. 업체면 companyId, 게시글이면 postId 중 하나만 채운다.
 * 같은 회원이 같은 대상을 두 번 저장할 수 없다 (DB UNIQUE).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long favoriteId;

	@Column(nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FavoriteTargetType targetType;

	/** targetType이 COMPANY일 때만 */
	private Long companyId;

	/** targetType이 POST일 때만 */
	private Long postId;

	public static Favorite ofCompany(Long memberId, Long companyId) {
		Favorite favorite = new Favorite();
		favorite.memberId = memberId;
		favorite.targetType = FavoriteTargetType.COMPANY;
		favorite.companyId = companyId;
		return favorite;
	}
}
