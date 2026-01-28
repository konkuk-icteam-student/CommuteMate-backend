package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FaqQueryRepository {
    Page<Faq> searchFaqs(
            Long teamId,
            Long categoryId,
            String keyword,
            FaqSearchScope searchScope,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
