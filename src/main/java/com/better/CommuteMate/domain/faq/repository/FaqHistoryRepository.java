package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FaqHistoryRepository extends JpaRepository<FaqHistory, Long> {

    void deleteByFaqIdAndEditedAt(Long faqId, LocalDate editedAt);

}
