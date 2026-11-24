package com.example.packhub.domain.post.service;

import com.example.packhub.domain.category.entity.CategoryDomain;
import com.example.packhub.domain.category.service.CategoryService;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.service.FileUsageValidator;
import com.example.packhub.domain.member.service.CompanyMemberValidator;
import com.example.packhub.domain.post.dto.RequestSaveRequest;
import com.example.packhub.domain.post.dto.RequestCreateResponse;
import com.example.packhub.domain.post.entity.AttachType;
import com.example.packhub.domain.post.entity.Post;
import com.example.packhub.domain.post.entity.PostAttachment;
import com.example.packhub.domain.post.repository.PostAttachmentRepository;
import com.example.packhub.domain.post.repository.PostRepository;
import com.example.packhub.domain.post.repository.PostRequestRepository;
import com.example.packhub.global.error.BusinessException;
import com.example.packhub.global.error.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import com.example.packhub.domain.file.entity.File;
import com.example.packhub.domain.file.service.FileService;
import com.example.packhub.domain.member.entity.Member;
import com.example.packhub.domain.member.repository.MemberRepository;
import com.example.packhub.domain.post.dto.RequestDetailResponse;
import com.example.packhub.domain.post.dto.RequestPageResponse;
import com.example.packhub.domain.post.dto.RequestSearchRequest;
import com.example.packhub.domain.post.dto.RequestUpdateResponse;
import com.example.packhub.domain.post.entity.PostStatus;
import com.example.packhub.domain.post.entity.PostRequest;
import com.example.packhub.domain.post.entity.PostType;
import com.example.packhub.domain.post.entity.PostVisibility;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의뢰하기 (Post 중 postType = REQUEST). 등록은 업체를 등록한 기업회원만 할 수 있다 (피그마 5-1·5-3).
 * 담당자 연락처는 글에 저장하지 않고 상세 조회 때 작성자 회원정보를 읽는다 (2026-11-23 결정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {

	/** 게시글 첨부로 받을 수 있는 파일 용도 */
	private static final Set<FilePurpose> ATTACHMENT_PURPOSES = Set.of(FilePurpose.POST_IMAGE, FilePurpose.POST_DOCUMENT);

	private final PostRepository postRepository;
	private final PostRequestRepository postRequestRepository;
	private final PostAttachmentRepository postAttachmentRepository;
	private final CompanyRepository companyRepository;
	private final CompanyMemberValidator companyMemberValidator;
	private final CategoryService categoryService;
	private final FileUsageValidator fileUsageValidator;
	private final FileService fileService;
	private final MemberRepository memberRepository;
	private final PostService postService;

	/**
	 * 등록하면 바로 게시(OPEN)된다. 저장 순서: Post(번호 받기) → PostRequest·PostAttachment(받은 번호 사용).
	 * 기업회원이 아니면 403 COMPANY_MEMBER_REQUIRED, 업체가 없으면 403 COMPANY_REGISTRATION_REQUIRED,
	 * 고를 수 없는 카테고리면 400 INVALID_CATEGORY, 같은 파일을 두 번 넣으면 400 INVALID_INPUT.
	 */
	@Transactional
	public RequestCreateResponse create(Long memberId, RequestSaveRequest request) {
		companyMemberValidator.validate(memberId);
		Company company = companyRepository.findByMemberIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_REGISTRATION_REQUIRED));
		categoryService.validateSelectable(request.categoryId(), CategoryDomain.COMMON);
		Map<Long, FilePurpose> filePurposes = validateFiles(memberId, request.fileIds());

		Post post = postRepository.save(request.toPost(memberId, company.getCompanyId()));
		postRequestRepository.save(request.toPostRequest(post.getPostId()));
		saveAttachments(post.getPostId(), request.fileIds(), filePurposes);
		return RequestCreateResponse.from(post);
	}

	/**
	 * 의뢰 수정 (5-5 "수정하기"). 받은 값으로 전체를 바꾸고, 첨부도 보낸 목록으로 바꾼다.
	 * 없거나 삭제된 의뢰는 404 POST_NOT_FOUND, 남의 글은 403 FORBIDDEN, 마감된 글은 409 POST_ALREADY_CLOSED.
	 */
	@Transactional
	public RequestUpdateResponse update(Long memberId, Long postId, RequestSaveRequest request) {
		Post post = postService.findMine(memberId, postId);
		if (post.getPostType() != PostType.REQUEST) {
			throw new BusinessException(ErrorCode.POST_NOT_FOUND);
		}
		if (post.getStatus() == PostStatus.CLOSED) {
			throw new BusinessException(ErrorCode.POST_ALREADY_CLOSED);
		}
		categoryService.validateSelectable(request.categoryId(), CategoryDomain.COMMON);
		Map<Long, FilePurpose> filePurposes = validateFiles(memberId, request.fileIds());

		request.applyTo(post);
		postRequestRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND))
			.update(request.toPostRequest(postId));
		postAttachmentRepository.deleteByPostId(postId);
		saveAttachments(postId, request.fileIds(), filePurposes);
		postRepository.flush();
		return RequestUpdateResponse.from(post);
	}

	/** 의뢰 목록. 게시 중(OPEN)인 의뢰만 검색어·카테고리로 찾고 정렬해서 한 페이지만 돌려준다 */
	public RequestPageResponse search(RequestSearchRequest request) {
		boolean allCategories = request.categoryId() == null;
		List<Long> categoryIds = allCategories ? List.of()
			: categoryService.findFilterIds(request.categoryId(), CategoryDomain.COMMON);
		return RequestPageResponse.of(postRepository.searchRequests(likePattern(request.keyword()), allCategories,
			categoryIds, request.sort().name(), LocalDate.now(), PageRequest.of(request.page() - 1, request.size())));
	}

	/**
	 * 의뢰 상세. 마감(CLOSED)된 의뢰도 볼 수 있다. 없거나 숨김·삭제된 의뢰는 404 POST_NOT_FOUND.
	 * 조회수는 쓰는 화면이 없어 기록하지 않는다 (2026-11-24 결정, 업체와 같음).
	 *
	 * @param memberId 로그인한 회원. 로그인하지 않았으면 null이고 isMine은 false다
	 */
	public RequestDetailResponse getDetail(Long postId, Long memberId) {
		Post post = postRepository
			.findByPostIdAndPostTypeAndVisibilityAndDeletedAtIsNull(postId, PostType.REQUEST, PostVisibility.VISIBLE)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		PostRequest detail = postRequestRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		Company company = companyRepository.findById(post.getCompanyId())
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		Member writer = memberRepository.findById(post.getMemberId())
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

		boolean isMine = post.getMemberId().equals(memberId);
		RequestDetailResponse.Contact contact = RequestDetailResponse.Contact.of(post, writer,
			post.isContactPublic() || isMine);
		return RequestDetailResponse.of(post, detail, isMine, RequestDetailResponse.Writer.of(post, company),
			contact, toAttachments(postId));
	}

	/** 첨부를 화면 순서대로. 지워진 파일은 빼고, 파일마다 유효 시간이 있는 조회 주소를 붙인다 */
	private List<RequestDetailResponse.Attachment> toAttachments(Long postId) {
		List<PostAttachment> attachments = postAttachmentRepository.findByPostIdOrderBySortOrderAsc(postId);
		Map<Long, File> files = fileService.findFiles(attachments.stream().map(PostAttachment::getFileId).toList())
			.stream().collect(Collectors.toMap(File::getFileId, Function.identity()));
		return attachments.stream()
			.filter(attachment -> files.containsKey(attachment.getFileId()))
			.map(attachment -> {
				File file = files.get(attachment.getFileId());
				return RequestDetailResponse.Attachment.of(file, attachment.getAttachType(),
					fileService.viewUrl(file.getStorageKey()));
			})
			.toList();
	}

	/** 검색어의 %, _를 글자 그대로 찾도록 '!'로 이스케이프하고 앞뒤에 %를 붙인다. 검색어가 없으면 null */
	private String likePattern(String keyword) {
		if (keyword == null) {
			return null;
		}
		return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
	}

	/** 본인이 게시글 용도로 올린 파일인지 확인하고, 파일마다 사진·문서 중 어느 용도인지 돌려준다 */
	private Map<Long, FilePurpose> validateFiles(Long memberId, List<Long> fileIds) {
		if (new HashSet<>(fileIds).size() != fileIds.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		return fileUsageValidator.validateAnyOf(memberId, ATTACHMENT_PURPOSES, fileIds);
	}

	/** 순서는 받은 목록의 순서(1부터), 종류는 업로드 용도로 정한다 */
	private void saveAttachments(Long postId, List<Long> fileIds, Map<Long, FilePurpose> filePurposes) {
		postAttachmentRepository.saveAll(IntStream.range(0, fileIds.size())
			.mapToObj(i -> {
				Long fileId = fileIds.get(i);
				AttachType type = filePurposes.get(fileId) == FilePurpose.POST_IMAGE ? AttachType.IMAGE : AttachType.DOCUMENT;
				return PostAttachment.of(postId, fileId, type, i + 1);
			})
			.toList());
	}
}
