package com.better.CommuteMate.faq.application.event;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqCategory;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.global.ai.rag.RagFaqIndexRequest;
import com.better.CommuteMate.global.ai.rag.RagServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * FAQ 저장/수정/삭제 트랜잭션 커밋 후
 * RAG 서비스에 청킹·임베딩을 위임하는 리스너.
 *
 * 인덱싱은 best-effort로 동작한다 — RAG 서비스 장애 시에도
 * FAQ 저장 자체는 이미 커밋되어 있으므로 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqIndexingListener {

    private final FaqRepository faqRepository;
    private final RagServiceClient ragServiceClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void handleIndex(FaqIndexEvent event) {
        try {
            Faq faq = faqRepository.findById(event.faqId()).orElse(null);

            if (faq == null || Boolean.TRUE.equals(faq.getDeletedFlag())) {
                return;
            }

            String plainContent = Jsoup.parse(faq.getContent()).text();
            String plainAnswer = Jsoup.parse(faq.getAnswer()).text();

            String text = "질문: " + plainContent + "\n\n답변: " + plainAnswer;

            List<Long> categoryIds = faq.getFaqCategories().stream()
                    .map(faqCategory -> faqCategory.getCategory().getId())
                    .toList();

            List<String> categoryNames = faq.getFaqCategories().stream()
                    .map(faqCategory -> faqCategory.getCategory().getName())
                    .toList();

            ragServiceClient.indexFaq(new RagFaqIndexRequest(
                    faq.getId(),
                    text,
                    faq.getTitle(),
                    categoryIds,
                    categoryNames,
                    faq.getUpdatedDate()
            ));

            log.info("FAQ {} RAG 인덱싱 완료", faq.getId());
        } catch (Exception e) {
            log.warn("FAQ {} RAG 인덱싱 실패: {}", event.faqId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDelete(FaqDeleteEvent event) {
        try {
            ragServiceClient.deleteFaq(event.faqId());

            log.info("FAQ {} RAG 청크 삭제 완료", event.faqId());
        } catch (Exception e) {
            log.warn("FAQ {} RAG 청크 삭제 실패: {}", event.faqId(), e.getMessage());
        }
    }
}
