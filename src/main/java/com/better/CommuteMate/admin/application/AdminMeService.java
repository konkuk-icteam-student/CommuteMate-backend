package com.better.CommuteMate.admin.application;

import com.better.CommuteMate.admin.controller.dto.AdminMeResponse;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMeService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public AdminMeResponse getMe(Long userId) {
        User admin = userRepository.findByUserId(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));
        Organization organization = organizationRepository.findById(admin.getOrganizationId())
                .orElseThrow(() -> CustomException.of(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
        return AdminMeResponse.of(admin, organization);
    }
}
