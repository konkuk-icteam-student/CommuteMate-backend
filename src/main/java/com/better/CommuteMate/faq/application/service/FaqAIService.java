package com.better.CommuteMate.faq.application.service;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.faq.application.ai.FaqCategoryPromptBuilder;
import com.better.CommuteMate.faq.application.ai.FaqCategoryResponseParser;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.better.CommuteMate.faq.application.dto.request.GetFaqAISearchRequest;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListResponse;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListWrapper;
import com.better.CommuteMate.global.ai.rag.RagFaqSearchResponse;
import com.better.CommuteMate.global.ai.rag.RagServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqAIService {

    private static final int CANDIDATE_LIMIT = 200;
    private static final int PAGE_SIZE = 10;

    private final CategoryRepository categoryRepository;
    private final ManagerCategoryRepository managerCategoryRepository;
    private final FaqRepository faqRepository;
    private final RagServiceClient ragServiceClient;
    private final FaqCategoryPromptBuilder categoryPromptBuilder;
    private final FaqCategoryResponseParser categoryResponseParser;

    public List<Category> recommendCategory(String title, String content) {
        List<Category> categories = categoryRepository.findAll();
        String prompt = categoryPromptBuilder.build(title, content, categories);

        String response;
        try {
            response = ragServiceClient.recommendCategory(prompt);
        } catch (Exception e) {
            log.warn("RAG 카테고리 추천 실패, 수동 선택으로 폴백: {}", e.getMessage());
            return List.of();
        }

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
            List<Long> categoryFilter = resolveCategoryFilter(organizationId, categoryId);

            if (organizationId != null && categoryFilter.isEmpty()) {
                return new GetFaqListWrapper(List.of(), page, 0);
            }

            RagFaqSearchResponse response = ragServiceClient.searchFaqs(
                    keyword,
                    categoryFilter,
                    startDate,
                    endDate,
                    CANDIDATE_LIMIT
            );

            List<Faq> ranked = loadRankedFaqs(response);

            return paginate(ranked, page);

        } catch (Exception e) {
            log.warn("RAG FAQ 검색 실패, 키워드 검색으로 폴백", e);

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

    /**
     * organizationId/categoryId를 RAG 서비스에 넘길
     * 카테고리 id 필터로 변환한다.
     *
     * organizationId는 해당 소속 매니저들이 배정된 카테고리 목록으로
     * 풀어서 전달한다 (기존 hybridSearch의 manager_category 조인 대체).
     */
    private List<Long> resolveCategoryFilter(Long organizationId, Long categoryId) {
        if (categoryId != null) {
            return List.of(categoryId);
        }

        if (organizationId != null) {
            return managerCategoryRepository.findCategoryIdsByOrganizationId(organizationId);
        }

        return List.of();
    }

    /**
     * RAG 검색 결과(faq_id, 관련도 순)를 실제 FAQ 원본으로 변환한다.
     * 삭제된 FAQ는 제외하고 관련도 순서를 유지한다.
     */
    private List<Faq> loadRankedFaqs(RagFaqSearchResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return List.of();
        }

        List<Long> rankedIds = response.results().stream()
                .map(RagFaqSearchResponse.RagFaqSearchItem::faqId)
                .toList();

        Map<Long, Faq> faqById = faqRepository.findAllById(rankedIds).stream()
                .collect(Collectors.toMap(Faq::getId, Function.identity()));

        List<Faq> ranked = new ArrayList<>();

        for (Long faqId : rankedIds) {
            Faq faq = faqById.get(faqId);

            if (faq != null && !Boolean.TRUE.equals(faq.getDeletedFlag())) {
                ranked.add(faq);
            }
        }

        return ranked;
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
