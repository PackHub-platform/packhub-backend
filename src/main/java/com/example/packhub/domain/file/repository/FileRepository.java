package com.example.packhub.domain.file.repository;

import com.example.packhub.domain.file.entity.File;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {

	Optional<File> findByStorageKey(String storageKey);
}
