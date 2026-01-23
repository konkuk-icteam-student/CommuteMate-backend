package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.faq.dto.request.PostFaqRequest;
import com.better.CommuteMate.faq.dto.request.PutFaqUpdateRequest;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.domain.faq.repository.FaqHistoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.faq.dto.response.PostFaqResponse;
import com.better.CommuteMate.faq.dto.response.PutFaqUpdateResponse;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.FaqException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.FaqErrorCode;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqHistoryRepository faqHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public PostFaqResponse createFaq(Long userId, PostFaqRequest request) {

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> CategoryException.of(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Faq faq = Faq.create(
                request.title(),
                request.complainantName(),
                request.content(),
                request.answer(),
                request.etc(),
                writer,
                category
        );

        faqRepository.save(faq);

        FaqHistory faqhistory = FaqHistory.create(faq);
        faqHistoryRepository.save(faqhistory);

        return new PostFaqResponse(faq.getId());
    }

    public PutFaqUpdateResponse updateFaq(Long userId, Long faqId, PutFaqUpdateRequest request) {
        User modifier = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> FaqException.of(FaqErrorCode.FAQ_NOT_FOUND));

        if (Boolean.TRUE.equals(faq.getDeletedFlag())) {
            throw FaqException.of(FaqErrorCode.FAQ_ALREADY_DELETED);
        }

        Category category = faq.getCategory();
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> CategoryException.of(CategoryErrorCode.CATEGORY_NOT_FOUND));
        }

        faq.update(
                request.title(),
                request.complainantName(),
                request.content(),
                request.answer(),
                request.etc(),
                category,
                modifier
        );

        faqRepository.save(faq);

        FaqHistory faqhistory = FaqHistory.create(faq);
        faqHistoryRepository.save(faqhistory);

        return new PutFaqUpdateResponse(faqId);
    }
}
