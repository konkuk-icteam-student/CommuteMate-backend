package com.better.CommuteMate.subcategory.application.dto;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.entity.SubCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.SubCategoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.SubCategoryException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.SubcategoryErrorCode;
import com.better.CommuteMate.subcategory.application.dto.request.PostSubCategoryRegisterRequest;
import com.better.CommuteMate.subcategory.application.dto.request.PostSubCategoryUpdateNameRequest;
import com.better.CommuteMate.subcategory.application.dto.response.DeleteSubCategoryResponse;
import com.better.CommuteMate.subcategory.application.dto.response.PostSubCategoryRegisterResponse;
import com.better.CommuteMate.subcategory.application.dto.response.PostSubCategoryUpdateNameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final FaqRepository faqRepository;

    public PostSubCategoryRegisterResponse registerSubCategory(PostSubCategoryRegisterRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (subCategoryRepository.existsByCategoryIdAndName(request.categoryId(), request.subCategoryName())) {
            throw new SubCategoryException(SubcategoryErrorCode.SUBCATEGORY_ALREADY_EXISTS);
        }

        SubCategory subCategory = SubCategory.builder()
                .name(request.subCategoryName())
                .category(category)
                .favorite(false)
                .build();

        subCategoryRepository.save(subCategory);

        return new PostSubCategoryRegisterResponse(subCategory.getId());
    }

    public PostSubCategoryUpdateNameResponse updateSubCategoryName(Long subCategoryId, PostSubCategoryUpdateNameRequest request) {

        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new SubCategoryException(SubcategoryErrorCode.SUBCATEGORY_NOT_FOUND));

        if (subCategoryRepository.existsByCategoryIdAndName(subCategory.getCategory().getId(), request.subCategoryName())) {
            throw new SubCategoryException(SubcategoryErrorCode.SUBCATEGORY_ALREADY_EXISTS);
        }

        subCategory.updateName(request.subCategoryName());

        return new PostSubCategoryUpdateNameResponse(subCategory.getId(), subCategory.getName());
    }

    public DeleteSubCategoryResponse deleteSubCategory(Long subCategoryId) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new SubCategoryException(SubcategoryErrorCode.SUBCATEGORY_NOT_FOUND));

        if (faqRepository.existsBySubCategoryId(subCategoryId)) {
            throw new SubCategoryException(SubcategoryErrorCode.SUBCATEGORY_DELETE_NOT_ALLOWED);
        }

        subCategoryRepository.delete(subCategory);

        return new DeleteSubCategoryResponse(subCategoryId);
    }
}