package com.better.CommuteMate.category.application;

import com.better.CommuteMate.category.application.dto.PostCategoryRegisterRequest;
import com.better.CommuteMate.category.application.dto.PostCategoryRegisterResponse;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public PostCategoryRegisterResponse registerCategory(PostCategoryRegisterRequest request) {
        Category category = Category.builder()
                .name(request.categoryName())
                .build();

        Category saved = categoryRepository.save(category);
        return new PostCategoryRegisterResponse(saved.getId());
    }
}
