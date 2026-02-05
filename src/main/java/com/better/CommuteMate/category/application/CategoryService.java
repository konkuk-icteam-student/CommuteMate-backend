package com.better.CommuteMate.category.application;

import com.better.CommuteMate.category.application.dto.request.PostCategoryRequest;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListResponse;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListWrapper;
import com.better.CommuteMate.category.application.dto.response.PostCategoryResponse;
import com.better.CommuteMate.category.application.dto.request.PutCategoryUpdateRequest;
import com.better.CommuteMate.category.application.dto.response.PutCategoryUpdateResponse;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FaqRepository faqRepository;
    private final ManagerCategoryRepository managerCategoryRepository;

    public PostCategoryResponse registerCategory(PostCategoryRequest request) {

        if (categoryRepository.existsByName(request.categoryName())) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = new Category(request.categoryName());

        Category saved = categoryRepository.save(category);

        return new PostCategoryResponse(saved.getId());
    }

    public PutCategoryUpdateResponse updateCategory(Long categoryId, PutCategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (categoryRepository.existsByName(request.categoryName())) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        category.updateName(request.categoryName());
        return new PutCategoryUpdateResponse(category.getId(), category.getName());
    }

    @Transactional(readOnly = true)
    public GetCategoryListWrapper getCategoryList() {
        List<Category> categories = categoryRepository.findAll();

        List<GetCategoryListResponse> result = categories.stream()
                .map(category -> new GetCategoryListResponse(category.getId(), category.getName()))
                .toList();

        return new GetCategoryListWrapper(result);
    }

    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (faqRepository.existsByCategoryId(categoryId)|| managerCategoryRepository.existsByCategoryId(categoryId)) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_DELETE_NOT_ALLOWED);
        }

        categoryRepository.delete(category);
    }

}
