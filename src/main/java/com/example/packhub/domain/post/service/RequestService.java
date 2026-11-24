package com.example.packhub.domain.post.service;

import com.example.packhub.domain.category.entity.CategoryDomain;
import com.example.packhub.domain.category.service.CategoryService;
import com.example.packhub.domain.company.entity.Company;
import com.example.packhub.domain.company.repository.CompanyRepository;
import com.example.packhub.domain.file.entity.FilePurpose;
import com.example.packhub.domain.file.service.FileUsageValidator;
import com.example.packhub.domain.member.service.CompanyMemberValidator;
import com.example.packhub.domain.post.dto.RequestCreateRequest;
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

	/**
	 * 등록하면 바로 게시(OPEN)된다. 저장 순서: Post(번호 받기) → PostRequest·PostAttachment(받은 번호 사용).
	 * 기업회원이 아니면 403 COMPANY_MEMBER_REQUIRED, 업체가 없으면 403 COMPANY_REGISTRATION_REQUIRED,
	 * 고를 수 없는 카테고리면 400 INVALID_CATEGORY, 같은 파일을 두 번 넣으면 400 INVALID_INPUT.
	 */
	@Transactional
	public RequestCreateResponse create(Long memberId, RequestCreateRequest request) {
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
