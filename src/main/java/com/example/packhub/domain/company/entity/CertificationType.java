package com.example.packhub.domain.company.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보유 인증 목록 (필터 통합 문서의 필터 5). 업체는 이 목록에서만 고를 수 있고 직접 입력은 없다.
 * DB에는 코드(상수 이름)를 저장하므로, 이미 쓰인 상수 이름은 바꾸지 않는다. 표시명·설명은 자유롭게 고쳐도 된다.
 */
@Getter
@RequiredArgsConstructor
public enum CertificationType {

	ISO_9001("품질경영 인증(ISO 9001)", "품질·공정 관리체계"),
	ISO_14001("환경경영 인증(ISO 14001)", "사업장 환경관리"),
	ISO_45001("안전보건경영 인증(ISO 45001)", "사업장 안전·보건관리"),
	HACCP("식품안전 인증(HACCP)", "해당 식품 제조·가공·포장 업무"),
	ISO_22000("식품안전경영 인증(ISO 22000)", "식품 공급망 안전관리"),
	FSSC_22000("식품안전 인증(FSSC 22000)", "인증범위에 포함된 포장재·공정"),
	BRCGS_PACKAGING("포장재 안전·품질 인증(BRCGS Packaging Materials)", "포장재 제조·가공 관리"),
	IFS_PACSECURE("포장재 안전·품질 인증(IFS PACsecure)", "포장재 제조·가공 관리"),
	SQF("식품포장재 제조 인증(SQF)", "해당 포장재 제조사업장"),
	KR_FOOD_CONTACT_TEST("국내 식품용 포장재 시험자료", "식품접촉 기준·규격 시험"),
	FDA_FOOD_CONTACT("미국 식품접촉 적합성 자료(FDA 관련)", "미국용 원료·제품의 사용 적합성"),
	EU_FOOD_CONTACT("EU 식품접촉 적합성 자료", "해당 EU 규정에 따른 자료"),
	GREEN_CERTIFICATION("녹색인증", "녹색기술·녹색기술제품·녹색전문기업"),
	ECO_LABEL("환경표지 인증", "해당 제품군의 환경성·품질 기준"),
	GR("우수재활용제품 인증(GR)", "해당 재활용제품의 품질"),
	EPD("환경성적표지 인증(EPD)", "제품 전 과정의 환경영향 정보"),
	LOW_CARBON("저탄소제품 인증", "해당 탄소 기준 충족"),
	FSC_COC("종이·목재 원료이력 인증(FSC CoC)", "종이·목재의 가공·유통 이력"),
	PEFC_KFCC_COC("종이·목재 원료이력 인증(PEFC·KFCC CoC)", "해당 산림원료 공급망"),
	ISCC_PLUS("재생·바이오 원료 공급망 인증(ISCC PLUS)", "대체 원료의 공급망 관리"),
	GRS("재생원료·가공관리 인증(GRS)", "재생원료와 해당 가공·공급망"),
	RCS("재생원료 이력 인증(RCS)", "재생원료의 공급망 이력"),
	INDUSTRIAL_COMPOST("산업용 퇴비화 인증", "OK compost INDUSTRIAL 등 해당 프로그램"),
	HOME_COMPOST("가정용 퇴비화 인증", "OK compost HOME 등 해당 프로그램"),
	ISO_13485("의료기기 품질경영 인증(ISO 13485)", "해당 의료기기 관련 품질관리"),
	ISO_15378("의약품 1차 포장재 제조관리 인증(ISO 15378)", "의약품에 직접 닿는 포장재"),
	ISO_22716("화장품 제조관리 인증(ISO 22716)", "해당 화장품 제조·충전 업무"),
	KS("국가표준 제품인증(KS)", "해당 인증대상 제품의 품질"),
	KCS_MACHINERY("산업기계 안전인증(KCs)", "해당 기계·방호장치·부품"),
	MACHINERY_SELF_SAFETY("산업기계 자율안전확인신고", "해당 인쇄기·컨베이어·로봇 등"),
	KC_ELECTRICAL("전기용품 안전인증(KC)", "해당 전기제품"),
	CE("EU 제품 적합성선언·CE 표시", "해당 기계·전기제품의 EU 적합성"),
	UN_DANGEROUS_GOODS("위험물 포장용기 검사자료(UN 관련)", "해당 위험물 운송용 포장"),
	IPPC_ISPM_15("수출 목재포장재 소독처리 자료(IPPC·ISPM 15 관련)", "해당 목상자·목재 팔레트"),
	KOLAS("공인시험기관 인정(KOLAS)", "인정범위에 포함된 시험능력");

	private final String label;
	private final String description;
}
