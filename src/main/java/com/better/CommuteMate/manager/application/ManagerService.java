package com.better.CommuteMate.manager.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.manager.repository.ManagerRepository;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.ManagerErrorCode;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.manager.application.dto.request.PostManagerRequest;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListResponse;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListWrapper;
import com.better.CommuteMate.manager.application.dto.response.PatchFavoriteManagerResponse;
import com.better.CommuteMate.manager.application.dto.response.PostManagerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final CategoryRepository categoryRepository;
    private final ManagerCategoryRepository managerCategoryRepository;
    private final OrganizationRepository organizationRepository;

    public PostManagerResponse registerManager(PostManagerRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new CustomException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        Manager manager = managerRepository.findByNameAndOrganizationAndPhonenum(request.name(), organization, request.phonenum())
                .orElseGet(() -> managerRepository.save(new Manager(request.name(), organization, request.phonenum())));

        if (managerCategoryRepository.existsByManagerIdAndCategoryId(manager.getId(), request.categoryId())) {
            throw new CustomException(ManagerErrorCode.MANAGER_CATEGORY_ALREADY_EXISTS);
        }

        ManagerCategory managerCategory = ManagerCategory.assign(manager, category);

        managerCategoryRepository.save(managerCategory);

        return new PostManagerResponse(manager.getId(), category.getId());
    }

    @Transactional(readOnly = true)
    public GetManagerListWrapper getManagerList(Long categoryId, Long organizationId, boolean favoriteOnly, String searchName) {
        Organization organization = null;

        if (organizationId != null) {
            organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new CustomException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        }

        List<ManagerCategory> managerCategories = managerCategoryRepository.getManagers(categoryId, organization, favoriteOnly, searchName);

        List<GetManagerListResponse> result = managerCategories.stream()
                .map(GetManagerListResponse::new)
                .toList();

        return new GetManagerListWrapper(result);
    }


    public PatchFavoriteManagerResponse updateFavorite(Long managerId, Long categoryId, boolean favorite) {
        ManagerCategory managerCategory = managerCategoryRepository
                .findByManagerIdAndCategoryId(managerId, categoryId)
                .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_CATEGORY_NOT_FOUND));

        managerCategory.updateFavorite(favorite);

        return new PatchFavoriteManagerResponse(managerCategory);
    }


    public void deleteManager(Long managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new CustomException(ManagerErrorCode.MANAGER_NOT_FOUND));

        managerCategoryRepository.deleteByManager(manager);

        managerRepository.delete(manager);
    }

}
