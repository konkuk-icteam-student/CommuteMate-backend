package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.SystemCreatedYearResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminSystemService {

    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public SystemCreatedYearResponse getCreatedYear(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> CustomException.of(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        LocalDateTime createdAt = organization.getCreatedAt();
        int year;
        if (createdAt != null) {
            year = createdAt.getYear();
        } else {
            // TODO: created_at null 시 처리 확인 필요
            year = LocalDateTime.now().getYear();
        }

        return new SystemCreatedYearResponse(year);
    }
}
