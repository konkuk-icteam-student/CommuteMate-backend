package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.UserErrorCode;
import com.better.CommuteMate.user.controller.dto.AdminUserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserSearchService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

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
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        Map<Long, UserProfile> profiles = userIds.isEmpty()
                ? Map.of()
                : userProfileRepository.findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));

        return AdminUserSearchResponse.from(users, profiles);
    }
}
