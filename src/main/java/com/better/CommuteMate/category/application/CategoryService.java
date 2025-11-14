package com.better.CommuteMate.category.application;

import com.better.CommuteMate.category.application.dto.PostCategoryRegisterRequest;
import com.better.CommuteMate.category.application.dto.PostCategoryRegisterResponse;
import com.better.CommuteMate.category.application.dto.PutCategoryUpdateRequest;
import com.better.CommuteMate.category.application.dto.PutCategoryUpdateResponse;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public PostCategoryRegisterResponse registerCategory(PostCategoryRegisterRequest request) {
        Category category = Category.builder()
                .name(request.categoryName())
                .build();

        Category saved = categoryRepository.save(category);
        return new PostCategoryRegisterResponse(saved.getId());
    }

    @Transactional
    public PutCategoryUpdateResponse updateCategory(Long categoryId, PutCategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (categoryRepository.existsByName(request.categoryName())) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        category.setName(request.categoryName());
        return new PutCategoryUpdateResponse(category.getId(), category.getName());
    }
}
