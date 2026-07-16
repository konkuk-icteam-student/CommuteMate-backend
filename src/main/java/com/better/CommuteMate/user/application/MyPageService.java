package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.faq.entity.FaqStatus;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.user.controller.dto.GetMyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {
    private final UserRepository userRepository;
    private final FaqRepository faqRepository;
    private final OrganizationRepository organizationRepository;

    public GetMyPageResponse getMyPage(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        CustomException.of(GlobalErrorCode.USER_NOT_FOUND)
                );

        Organization organization = organizationRepository
                .findById(user.getOrganizationId())
                .orElseThrow(() ->
                        CustomException.of(OrganizationErrorCode.ORGANIZATION_NOT_FOUND)
                );

        long publishedCount = faqRepository
                .countByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.PUBLISHED
                );

        long draftCount = faqRepository
                .countByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.DRAFT
                );

        return new GetMyPageResponse(
                user.getName(),
                user.getEmail(),
                organization.getId(),
                organization.getName(),
                publishedCount,
                draftCount
        );
    }

    public Object getMyPublishedFaqs(
            Long userId,
            int page
    ) {

        return null;
    }

    public Object getMyDraftFaqs(
            Long userId,
            int page
    ) {

        return null;
    }
}
