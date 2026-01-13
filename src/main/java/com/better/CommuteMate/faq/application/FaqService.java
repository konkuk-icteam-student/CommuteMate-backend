package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.faq.dto.request.FaqCreateRequest;
import com.better.CommuteMate.domain.faq.dto.request.FaqUpdateRequest;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.domain.faq.repository.FaqHistoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.FaqErrorCode;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
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
    private final UserRepository userRepository;

    public void createFaq(FaqCreateRequest request) {

        // Todo 임시 인증 로직
        User writer = userRepository.findById(request.userId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository.findByName(request.category())
                .orElseThrow(() -> BasicException.of(CategoryErrorCode.CATEGORY_NOT_FOUND));


        Faq faq = Faq.builder()
                .category(category)
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
                .orElseThrow(() -> BasicException.of(FaqErrorCode.FAQ_NOT_FOUND));

        // 해당 FAQ 삭제 여부 확인
        if (Boolean.TRUE.equals(faq.getDeletedFlag())) {
            throw BasicException.of(FaqErrorCode.FAQ_ALREADY_DELETED);
        }

        // 수정자 조회
        // Todo 임시 인증 로직. 토큰 활용 안정화되면 삭제 예정
        User editor = userRepository.findById(request.userId())
                .orElseThrow(() -> BasicException.of(GlobalErrorCode.USER_NOT_FOUND));

        // 히스토리 저장
        FaqHistory history = FaqHistory.builder()
                .faq(faq)
                .title(faq.getTitle())
                .category(faq.getCategory().getName())
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
