package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.entity.SubCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.SubCategoryRepository;
import com.better.CommuteMate.domain.faq.dto.request.FaqCreateRequest;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;

    public void createFaq(FaqCreateRequest request) {
        User writer = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다.")); // Todo 임시 로직

        Category category = categoryRepository.findByName(request.category())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대분류입니다."));

        SubCategory subCategory = subCategoryRepository
                .findByNameAndCategory(request.subCategory(), category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 소분류입니다."));

        Faq faq = Faq.builder()
                .subCategory(subCategory)
                .title(request.title())
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .etc(request.etc())

                // 작성자, 수정자 (최초 동일)
                .writer(writer)
                .writerName(writer.getName())
                .lastEditor(writer)
                .lastEditorName(writer.getName())

                // 관리 필드
                .manager(writer.getName())   // Todo 관리자 표시 화면 나오면 로직 수정할 것
                .deletedFlag(false)
                .build();

        faqRepository.save(faq);
    }
}
