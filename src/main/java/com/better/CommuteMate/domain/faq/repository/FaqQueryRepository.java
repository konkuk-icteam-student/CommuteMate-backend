package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface FaqQueryRepository {
    Page<Faq> searchFaqs(
            Long organizationId,
            Long categoryId,
            String keyword,
            FaqSearchScope searchScope,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<Faq> hybridSearch(
            Long organizationId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            float[] embedding,
            int limit
    );

}
