package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.entity.SubCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.SubCategoryRepository;
import com.better.CommuteMate.domain.faq.dto.request.FaqCreateRequest;
import com.better.CommuteMate.domain.faq.dto.request.FaqUpdateRequest;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.domain.faq.repository.FaqHistoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqHistoryRepository faqHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;

    public void createFaq(FaqCreateRequest request) {

        // Todo 임시 인증 로직
        User writer = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

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

    public void updateFaq(Long faqId, FaqUpdateRequest request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 FAQ입니다."));

        // 해당 FAQ 삭제 여부 확인
        if (Boolean.TRUE.equals(faq.getDeletedFlag())) {
            throw new IllegalStateException("삭제된 FAQ는 수정할 수 없습니다.");
        }

        // 수정자 조회
        // Todo 임시 인증 로직. 토큰 활용 안정화되면 삭제 예정
        User editor = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 히스토리 저장
        FaqHistory history = FaqHistory.builder()
                .faq(faq)
                .title(faq.getTitle())
                .category(faq.getSubCategory().getCategory().getName())
                .subCategory(faq.getSubCategory().getName())
                .content(faq.getContent())
                .attachmentUrl(faq.getAttachmentUrl())
                .manager(faq.getManager())
                .writerName(faq.getWriterName())
                .editorName(faq.getLastEditorName())
                .editedAt(LocalDateTime.now())
                .build();

        faqHistoryRepository.save(history);

        // FAQ 최신 상태로 업데이트
        faq.setTitle(request.title());
        faq.setContent(request.content());
        faq.setEtc(request.etc());
        faq.setAttachmentUrl(request.attachmentUrl());
        faq.setManager(request.manager());

        faq.setLastEditor(editor);
        faq.setLastEditorName(request.editorName());
        faq.setLastEditedAt(LocalDateTime.now());
    }
}
