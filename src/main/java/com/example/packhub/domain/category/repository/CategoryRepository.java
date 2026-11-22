package com.example.packhub.domain.category.repository;

import com.example.packhub.domain.category.entity.Category;
import com.example.packhub.domain.category.entity.CategoryDomain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findByDomainAndIsActiveTrueOrderBySortOrderAscCategoryIdAsc(CategoryDomain domain);
}
