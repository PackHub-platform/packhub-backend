package com.example.packhub.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 의뢰하기 전용 정보 (Post와 1:1).
 * PK를 스스로 만들지 않고, 먼저 저장한 Post의 postId를 그대로 받아 쓴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostRequest {

	@Id
	private Long postId;

	@Column(length = 100)
	private String item;

	private Integer quantity;

	@Column(length = 10)
	private String quantityUnit;

	/** 희망 가격(원) */
	private Long desiredPrice;

	@Column(length = 100)
	private String packageType;

	@Column(length = 100)
	private String contents;

	@Column(length = 100)
	private String spec;

	@Column(length = 100)
	private String materialThickness;

	@Column(length = 100)
	private String printSpec;

	@Column(length = 200)
	private String materialComposition;

	@Column(length = 50)
	private String artworkProvided;

	@Column(length = 50)
	private String existingSample;

	private LocalDate desiredDeliveryDate;

	private LocalDate deadlineDate;

	@Column(length = 100)
	private String deliveryRegion;

	@Column(length = 100)
	private String deliveryMethod;

	@Column(length = 100)
	private String priceTerms;

	@Column(length = 100)
	private String paymentTerms;

	@Builder
	private PostRequest(Long postId, String item, Integer quantity, String quantityUnit, Long desiredPrice,
		String packageType, String contents, String spec, String materialThickness, String printSpec,
		String materialComposition, String artworkProvided, String existingSample, LocalDate desiredDeliveryDate,
		LocalDate deadlineDate, String deliveryRegion, String deliveryMethod, String priceTerms, String paymentTerms) {
		this.postId = postId;
		this.item = item;
		this.quantity = quantity;
		this.quantityUnit = quantityUnit;
		this.desiredPrice = desiredPrice;
		this.packageType = packageType;
		this.contents = contents;
		this.spec = spec;
		this.materialThickness = materialThickness;
		this.printSpec = printSpec;
		this.materialComposition = materialComposition;
		this.artworkProvided = artworkProvided;
		this.existingSample = existingSample;
		this.desiredDeliveryDate = desiredDeliveryDate;
		this.deadlineDate = deadlineDate;
		this.deliveryRegion = deliveryRegion;
		this.deliveryMethod = deliveryMethod;
		this.priceTerms = priceTerms;
		this.paymentTerms = paymentTerms;
	}
}
