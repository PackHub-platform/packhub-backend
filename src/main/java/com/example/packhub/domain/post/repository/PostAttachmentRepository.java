package com.example.packhub.domain.post.repository;

import com.example.packhub.domain.post.entity.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {
}
