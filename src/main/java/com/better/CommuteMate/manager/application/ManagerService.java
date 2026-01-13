package com.better.CommuteMate.manager.application;

import com.better.CommuteMate.category.application.dto.request.PutManagerCategoryRequest;
import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.category.application.dto.request.PostManagerCategoryRequest;
import com.better.CommuteMate.category.application.dto.response.PostManagerCategoryResponse;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.ManagerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagerService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ManagerCategoryRepository managerCategoryRepository;

    public void registerManager(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        if (user.getRoleCode() == CodeType.RL02) { // 이미 관리자라면 예외 발생시킴
            throw BasicException.of(ManagerErrorCode.ALREADY_MANAGER);
        }

        user.setRoleCode(CodeType.RL02); // RL02 = ADMIN
    }

    // 관리자-분류 매핑 등록
    public PostManagerCategoryResponse registerMappings(PostManagerCategoryRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        int count = 0;
        for (Integer categoryId : request.categoryIds()) {
            if (managerCategoryRepository.existsByManager_UserIdAndCategory_Id(manager.getUserId(), categoryId)) {
                throw BasicException.of(ManagerErrorCode.MANAGER_CATEGORY_ALREADY_EXISTS);
            }
            var category = categoryRepository.findById(Long.valueOf(categoryId))
                    .orElseThrow(() -> BasicException.of(GlobalErrorCode.NOT_FOUND));
            ManagerCategory mapping = ManagerCategory.of(manager, category);
            managerCategoryRepository.save(mapping);
            count++;
        }

        return new PostManagerCategoryResponse(count);
    }

    public void updateManagerCategories(PutManagerCategoryRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 기존 매핑 전체 삭제
        managerCategoryRepository.deleteAllByManager(manager);

        // 새로 등록
        for (String categoryName : request.categoryNames()) {
            var category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> BasicException.of(GlobalErrorCode.NOT_FOUND));

            ManagerCategory mapping = ManagerCategory.of(manager, category);
            managerCategoryRepository.save(mapping);
        }
    }

    // Todo dto 맞추어 수정하기
    public void deleteManagerCategories(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 기존 매핑 전체 삭제
        managerCategoryRepository.deleteAllByManager(manager);
    }

    public void revokeManagerRole(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 현재 역할이 MANAGER인지 확인
        if (!manager.getRoleCode().equals(CodeType.RL02)) {
            throw BasicException.of(ManagerErrorCode.MANAGER_ROLE_NOT_ASSIGNED);
        }

        // 해당되는 매니저의 Category 매핑 모두 삭제
        managerCategoryRepository.deleteAllByManager(manager);

        // 역할을 일반 사용자로 변경 (RL01: STUDENT)
        manager.setRoleCode(CodeType.RL01);
        userRepository.save(manager);
    }
}
