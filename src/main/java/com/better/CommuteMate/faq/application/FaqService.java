package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.manager.repository.ManagerRepository;
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
import com.better.CommuteMate.global.exceptions.ManagerException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.ManagerErrorCode;
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
    private final ManagerRepository managerRepository;
    private final ManagerCategoryRepository managerCategoryRepository;

    public PostFaqResponse createFaq(Long userId, PostFaqRequest request) {

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> CategoryException.of(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Manager manager = managerRepository.findById(request.managerId())
                .orElseThrow(() -> ManagerException.of(ManagerErrorCode.MANAGER_NOT_FOUND));

        if (!managerCategoryRepository
                .existsByManagerIdAndCategoryId(manager.getId(), category.getId())) {
            throw ManagerException.of(ManagerErrorCode.MANAGER_CATEGORY_MISMATCH);
        }

        Faq faq = Faq.create(
                request.title(),
                request.complainantName(),
                request.content(),
                request.answer(),
                request.etc(),
                writer,
                category,
                manager
        );

        faqRepository.save(faq);

        FaqHistory faqhistory = FaqHistory.create(faq, writer.getName());
        faqHistoryRepository.save(faqhistory);

        return new PostFaqResponse(faq.getId());
    }

    public PutFaqUpdateResponse updateFaq(Long faqId, PutFaqUpdateRequest request) {

        return new PutFaqUpdateResponse(faqId);
    }
}
