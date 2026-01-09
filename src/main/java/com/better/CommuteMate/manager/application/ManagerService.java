package com.better.CommuteMate.manager.application;

import com.better.CommuteMate.category.application.dto.request.PutManagerSubCategoryRequest;
import com.better.CommuteMate.domain.category.entity.ManagerSubCategory;
import com.better.CommuteMate.domain.category.repository.ManagerSubCategoryRepository;
import com.better.CommuteMate.domain.category.repository.SubCategoryRepository;
import com.better.CommuteMate.category.application.dto.request.PostManagerSubCategoryRequest;
import com.better.CommuteMate.category.application.dto.response.PostManagerSubCategoryResponse;
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
    private final SubCategoryRepository subCategoryRepository;
    private final ManagerSubCategoryRepository managerSubCategoryRepository;


    @Transactional
    public void registerManager(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        if (user.getRoleCode() == CodeType.RL02) { // 이미 관리자라면 예외 발생시킴
            throw BasicException.of(ManagerErrorCode.ALREADY_MANAGER);
        }

        user.setRoleCode(CodeType.RL02); // RL02 = ADMIN
    }

    // 관리자-소분류 매핑 등록
    @Transactional
    public PostManagerSubCategoryResponse registerMappings(PostManagerSubCategoryRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        int count = 0;
        for (Integer subCategoryId : request.subCategoryIds()) {
            if (managerSubCategoryRepository.existsByManager_UserIdAndSubCategory_Id(manager.getUserId(), subCategoryId)) {
                throw BasicException.of(ManagerErrorCode.MANAGER_CATEGORY_ALREADY_EXISTS);
            }
            var subCategory = subCategoryRepository.findById(Long.valueOf(subCategoryId))
                    .orElseThrow(() -> BasicException.of(GlobalErrorCode.NOT_FOUND));
            ManagerSubCategory mapping = ManagerSubCategory.of(manager, subCategory);
            managerSubCategoryRepository.save(mapping);
            count++;
        }

        return new PostManagerSubCategoryResponse(count);
    }

    @Transactional
    public void updateManagerSubCategories(PutManagerSubCategoryRequest request) {
        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 기존 매핑 전체 삭제
        managerSubCategoryRepository.deleteAllByManager(manager);

        // 새로 등록
        for (String subCategoryName : request.subCategoryNames()) {
            var subCategory = subCategoryRepository.findByName(subCategoryName)
                    .orElseThrow(() -> BasicException.of(GlobalErrorCode.NOT_FOUND));

            ManagerSubCategory mapping = ManagerSubCategory.of(manager, subCategory);
            managerSubCategoryRepository.save(mapping);
        }
    }

    @Transactional // Todo dto 맞추어 수정하기
    public void deleteManagerSubCategories(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 기존 매핑 전체 삭제
        managerSubCategoryRepository.deleteAllByManager(manager);
    }

    @Transactional
    public void revokeManagerRole(Integer managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 현재 역할이 MANAGER인지 확인
        if (!manager.getRoleCode().equals(CodeType.RL02)) {
            throw BasicException.of(ManagerErrorCode.MANAGER_ROLE_NOT_ASSIGNED);
        }

        // 해당되는 매니저의 SubCategory 매핑 모두 삭제
        managerSubCategoryRepository.deleteAllByManager(manager);

        // 역할을 일반 사용자로 변경 (RL01: STUDENT)
        manager.setRoleCode(CodeType.RL01);
        userRepository.save(manager);
    }
}
