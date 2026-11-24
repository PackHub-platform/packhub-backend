package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostRequest;
import com.example.packhub.domain.post.entity.PostType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 의뢰하기 등록 요청 (피그마 5-3). 첨부자료 말고 모든 칸이 필수다 (디자이너 답, 2026-11-22).
 * 작성자·업체·담당자 연락처는 받지 않는다. 서버가 로그인 정보와 DB에서 정한다.
 * 파일은 먼저 파일 업로드 API로 올리고 여기에는 fileId만 담는다. 목록의 순서가 화면 표시 순서이고,
 * 사진·문서 구분은 업로드할 때 고른 용도(post-image, post-document)로 정한다.
 *
 * @param categoryId      카테고리 소분류 ID (GET /api/v1/categories?domain=COMMON)
 * @param isContactPublic "개인정보 공개" 토글. 끄면 상세에서 의뢰자 정보를 숨긴다
 * @param quantityUnit    수량 단위 (예: 매)
 * @param desiredPrice    희망 가격(원)
 * @param fileIds         첨부자료 (최대 10개). 없으면 빈 배열
 */
public record RequestCreateRequest(
	@NotNull Long categoryId,
	@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 1500) String content,
	@NotNull Boolean isContactPublic,

	@NotBlank @Size(max = 100) String item,
	@NotNull @Positive Integer quantity,
	@NotBlank @Size(max = 10) String quantityUnit,
	@NotNull @PositiveOrZero Long desiredPrice,

	@NotBlank @Size(max = 100) String packageType,
	@NotBlank @Size(max = 100) String contents,
	@NotBlank @Size(max = 100) String spec,
	@NotBlank @Size(max = 100) String materialThickness,
	@NotBlank @Size(max = 100) String printSpec,
	@NotBlank @Size(max = 200) String materialComposition,
	@NotBlank @Size(max = 50) String artworkProvided,
	@NotBlank @Size(max = 50) String existingSample,

	@NotNull @FutureOrPresent LocalDate desiredDeliveryDate,
	@NotNull @FutureOrPresent LocalDate deadlineDate,
	@NotBlank @Size(max = 100) String deliveryRegion,
	@NotBlank @Size(max = 100) String deliveryMethod,
	@NotBlank @Size(max = 100) String priceTerms,
	@NotBlank @Size(max = 100) String paymentTerms,

	@Size(max = 10) List<@NotNull Long> fileIds
) {

	/** 첨부를 보내지 않으면 빈 목록으로 본다 */
	public RequestCreateRequest {
		fileIds = fileIds == null ? List.of() : fileIds;
	}

	/** 공통 정보. 작성자·업체는 요청이 아니라 로그인 정보와 DB에서 받은 값으로 채운다 */
	public Post toPost(Long memberId, Long companyId) {
		return Post.builder()
			.memberId(memberId)
			.companyId(companyId)
			.postType(PostType.REQUEST)
			.categoryId(categoryId)
			.title(title)
			.content(content)
			.isContactPublic(isContactPublic)
			.build();
	}

	/** 의뢰 전용 정보. Post를 먼저 저장해서 받은 postId를 넣는다 */
	public PostRequest toPostRequest(Long postId) {
		return PostRequest.builder()
			.postId(postId)
			.item(item)
			.quantity(quantity)
			.quantityUnit(quantityUnit)
			.desiredPrice(desiredPrice)
			.packageType(packageType)
			.contents(contents)
			.spec(spec)
			.materialThickness(materialThickness)
			.printSpec(printSpec)
			.materialComposition(materialComposition)
			.artworkProvided(artworkProvided)
			.existingSample(existingSample)
			.desiredDeliveryDate(desiredDeliveryDate)
			.deadlineDate(deadlineDate)
			.deliveryRegion(deliveryRegion)
			.deliveryMethod(deliveryMethod)
			.priceTerms(priceTerms)
			.paymentTerms(paymentTerms)
			.build();
	}
}
