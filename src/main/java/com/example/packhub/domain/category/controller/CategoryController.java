package com.example.packhub.domain.category.controller;

import com.example.packhub.domain.category.dto.CategoryResponse;
import com.example.packhub.domain.category.entity.CategoryDomain;
import com.example.packhub.domain.category.service.CategoryService;
import com.example.packhub.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "분류")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@Operation(summary = "분류 목록 조회",
		description = "대분류와 그 아래 소분류를 돌려준다. 업체·의뢰·외주는 domain=COMMON을 쓴다. 로그인 없이 조회할 수 있다.")
	@GetMapping
	public ApiResponse<List<CategoryResponse>> getCategories(@RequestParam CategoryDomain domain) {
		return ApiResponse.ok(categoryService.getCategories(domain));
	}
}
