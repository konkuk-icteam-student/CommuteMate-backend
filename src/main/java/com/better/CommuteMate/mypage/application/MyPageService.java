package com.better.CommuteMate.mypage.application;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqStatus;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.mypage.dto.GetMyFaqListResponse;
import com.better.CommuteMate.mypage.dto.GetMyFaqListWrapper;
import com.better.CommuteMate.mypage.dto.GetMyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public GetMyFaqListWrapper getMyPublishedFaqs(
            Long userId,
            int page
    ) {
        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "updatedDate")
        );

        Page<Faq> faqPage =
                faqRepository.findByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.PUBLISHED,
                        pageable
                );

        List<GetMyFaqListResponse> faqs = faqPage.getContent()
                .stream()
                .map(GetMyFaqListResponse::new)
                .toList();

        return new GetMyFaqListWrapper(
                faqs,
                faqPage.getNumber(),
                faqPage.getTotalPages(),
                faqPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public GetMyFaqListWrapper getMyDraftFaqs(
            Long userId,
            int page
    ) {
        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "updatedDate")
        );

        Page<Faq> faqPage =
                faqRepository.findByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.DRAFT,
                        pageable
                );

        List<GetMyFaqListResponse> faqs = faqPage.getContent()
                .stream()
                .map(GetMyFaqListResponse::new)
                .toList();

        return new GetMyFaqListWrapper(
                faqs,
                faqPage.getNumber(),
                faqPage.getTotalPages(),
                faqPage.getTotalElements()
        );
    }
}
