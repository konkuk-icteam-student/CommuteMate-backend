package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.UserErrorCode;
import com.better.CommuteMate.user.controller.dto.AdminUserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserSearchService {

    private final UserRepository userRepository;

    public AdminUserSearchResponse search(Long organizationId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw CustomException.of(UserErrorCode.ADMIN_USER_SEARCH_KEYWORD_REQUIRED);
        }

        List<User> users = userRepository
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
                        organizationId,
                        CodeType.RL01,
                        keyword.trim()
                );
        return AdminUserSearchResponse.from(users);
    }
}
