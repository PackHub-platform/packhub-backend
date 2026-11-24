package com.example.packhub.domain.post.dto;

import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.post.entity.AttachType;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostRequest;
import com.example.packhub.domain.post.entity.PostStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 의뢰 상세 (피그마 5-2, 내 글이면 5-5). 수정 화면도 이 응답으로 불러온다.
 * 의뢰 칸들은 등록 요청(RequestSaveRequest)과 같은 이름으로 한 층에 둔다.
 *
 * @param isMine  로그인한 회원이 쓴 글이면 true (수정·삭제 버튼 표시)
 * @param favorited 로그인한 회원이 관심으로 저장했으면 true. 로그인하지 않았으면 false
 * @param writer  작성 업체 (업체 등록 정보)
 * @param contact 의뢰자 정보. 작성자의 현재 회원정보에서 읽는다
 */
public record RequestDetailResponse(
	Long postId,
	String title,
	String content,
	PostStatus status,
	Long categoryId,
	LocalDateTime createdAt,
	LocalDateTime closedAt,
	boolean isMine,
	boolean favorited,
	Writer writer,
	Contact contact,
	String item,
	Integer quantity,
	String quantityUnit,
	Long desiredPrice,
	String packageType,
	String contents,
	String spec,
	String materialThickness,
	String printSpec,
	String materialComposition,
	String artworkProvided,
	String existingSample,
	LocalDate desiredDeliveryDate,
	LocalDate deadlineDate,
	String deliveryRegion,
	String deliveryMethod,
	String priceTerms,
	String paymentTerms,
	List<Attachment> attachments
) {

	/** 작성 업체. 업체명을 누르면 companyId로 업체 상세에 간다 */
	public record Writer(Long memberId, Long companyId, String name, String industry) {

		public static Writer of(Post post, Company company) {
			return new Writer(post.getMemberId(), company.getCompanyId(), company.getName(), company.getIndustry());
		}
	}

	/**
	 * 의뢰자 정보. "개인정보 공개"를 끈 글은 본인이 아니면 isPublic 말고 모두 null이다.
	 * 연락시간(hours)은 입력할 곳이 정해지지 않아 지금은 항상 null이다.
	 */
	public record Contact(boolean isPublic, String name, String phone, String email, String hours) {

		public static Contact of(Post post, Member writer, boolean visible) {
			if (!visible) {
				return new Contact(post.isContactPublic(), null, null, null, null);
			}
			return new Contact(post.isContactPublic(), writer.getName(), writer.getPhone(), writer.getEmail(), null);
		}
	}

	/** 첨부 하나. url은 유효 시간이 있는 조회용 주소라 저장해 두고 다시 쓰지 않는다 */
	public record Attachment(Long fileId, AttachType attachType, String originalName, Long sizeBytes, String url) {

		public static Attachment of(File file, AttachType attachType, String url) {
			return new Attachment(file.getFileId(), attachType, file.getOriginalName(), file.getSizeBytes(), url);
		}
	}

	public static RequestDetailResponse of(Post post, PostRequest detail, boolean isMine, boolean favorited, Writer writer,
		Contact contact, List<Attachment> attachments) {
		return new RequestDetailResponse(post.getPostId(), post.getTitle(), post.getContent(), post.getStatus(),
			post.getCategoryId(), post.getCreatedAt(), post.getClosedAt(), isMine, favorited, writer, contact,
			detail.getItem(), detail.getQuantity(), detail.getQuantityUnit(), detail.getDesiredPrice(),
			detail.getPackageType(), detail.getContents(), detail.getSpec(), detail.getMaterialThickness(),
			detail.getPrintSpec(), detail.getMaterialComposition(), detail.getArtworkProvided(),
			detail.getExistingSample(), detail.getDesiredDeliveryDate(), detail.getDeadlineDate(),
			detail.getDeliveryRegion(), detail.getDeliveryMethod(), detail.getPriceTerms(), detail.getPaymentTerms(),
			attachments);
	}
}
