package com.better.CommuteMate.category.application;

import com.better.CommuteMate.category.application.dto.request.PostCategoryRegisterRequest;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListResponse;
import com.better.CommuteMate.category.application.dto.response.PatchFavoriteCategoryResponse;
import com.better.CommuteMate.category.application.dto.response.PostCategoryRegisterResponse;
import com.better.CommuteMate.category.application.dto.request.PutCategoryUpdateRequest;
import com.better.CommuteMate.category.application.dto.response.PutCategoryUpdateResponse;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final FaqRepository faqRepository;

    public PostCategoryRegisterResponse registerCategory(PostCategoryRegisterRequest request) {

        if (categoryRepository.existsByName(request.categoryName())) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.builder()
                .name(request.categoryName())
                .build();

        Category saved = categoryRepository.save(category);

        return new PostCategoryRegisterResponse(saved.getId());
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
    public List<GetCategoryListResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(category -> new GetCategoryListResponse(
                        category.getId(),
                        category.getName()
                ))
                .collect(Collectors.toList());
    }

    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (faqRepository.existsByCategoryId(categoryId)) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_DELETE_NOT_ALLOWED);
        }

        categoryRepository.delete(category);
    }

    public PatchFavoriteCategoryResponse updateFavorite(Long categoryId, boolean favorite) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        category.updateFavorite(favorite);

        return new PatchFavoriteCategoryResponse(category.getId(), category.isFavorite());
    }
}
