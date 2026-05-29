package com.better.CommuteMate.faq.application.service;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.faq.application.ai.FaqCategoryPromptBuilder;
import com.better.CommuteMate.faq.application.ai.FaqCategoryResponseParser;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.better.CommuteMate.faq.application.dto.request.GetFaqAISearchRequest;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListResponse;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListWrapper;
import com.better.CommuteMate.global.ai.OpenAIClient;
import com.better.CommuteMate.global.ai.OpenAIEmbeddingClient;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqAIService {

    private static final int CANDIDATE_LIMIT = 200;
    private static final int PAGE_SIZE = 10;

    private final CategoryRepository categoryRepository;
    private final FaqRepository faqRepository;
    private final OpenAIClient openAIClient;
    private final OpenAIEmbeddingClient embeddingClient;
    private final FaqCategoryPromptBuilder categoryPromptBuilder;
    private final FaqCategoryResponseParser categoryResponseParser;

    public List<Category> recommendCategory(String title, String content) {
        List<Category> categories = categoryRepository.findAll();
        String prompt = categoryPromptBuilder.build(title, content, categories);
        String response = openAIClient.call(prompt);
        List<String> names = categoryResponseParser.parse(response);
        return categoryRepository.findByNameIn(names);
    }

    public GetFaqListWrapper search(GetFaqAISearchRequest request) {
        String keyword = request.keyword();
        Long organizationId = request.organizationId();
        Long categoryId = request.categoryId();
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        int page = request.page() == null ? 0 : request.page();

        if (keyword == null || keyword.isBlank()) {
            return fallbackSearch(
                    organizationId,
                    categoryId,
                    keyword,
                    startDate,
                    endDate,
                    page
            );
        }

        try {
            float[] embedding = embeddingClient.embed(keyword);

            List<Faq> ranked = faqRepository.hybridSearch(
                                    organizationId,
                                    categoryId,
                                    startDate,
                                    endDate,
                                    keyword,
                                    embedding,
                                    CANDIDATE_LIMIT
                                );

            return paginate(ranked, page);

        } catch (Exception e) {
            log.warn("Hybrid Search 실패", e);

            return fallbackSearch(
                    organizationId,
                    categoryId,
                    keyword,
                    startDate,
                    endDate,
                    page
            );
        }
    }

    private GetFaqListWrapper paginate(
            List<Faq> ranked,
            int page
    ) {

        int totalPages = (int) Math.ceil((double) ranked.size() / PAGE_SIZE);

        int fromIndex = page * PAGE_SIZE;

        if (fromIndex >= ranked.size()) {
            return new GetFaqListWrapper(List.of(), page, totalPages);
        }

        int toIndex = Math.min(fromIndex + PAGE_SIZE, ranked.size());

        List<GetFaqListResponse> pageContent =
                ranked.subList(fromIndex, toIndex)
                        .stream()
                        .map(GetFaqListResponse::new)
                        .toList();

        return new GetFaqListWrapper(
                pageContent,
                page,
                totalPages
        );
    }

    private GetFaqListWrapper fallbackSearch(
            Long organizationId,
            Long categoryId,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page
    ) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Page<Faq> faqPage = faqRepository.searchFaqs(
                organizationId,
                categoryId,
                keyword,
                FaqSearchScope.TITLE_CONTENT,
                startDate,
                endDate,
                pageable
        );

        List<GetFaqListResponse> content = faqPage.getContent()
                .stream()
                .map(GetFaqListResponse::new)
                .toList();

        return new GetFaqListWrapper(
                content,
                faqPage.getNumber(),
                faqPage.getTotalPages()
        );
    }
}
