package com.example.packhub.domain.post.repository;

import com.example.packhub.domain.post.entity.PostRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRequestRepository extends JpaRepository<PostRequest, Long> {
}
