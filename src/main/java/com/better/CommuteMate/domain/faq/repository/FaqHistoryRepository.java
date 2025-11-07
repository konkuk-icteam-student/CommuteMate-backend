package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqHistoryRepository extends JpaRepository<FaqHistory, Long> {

    // 특정 FAQ에 대한 모든 히스토리 조회
    List<FaqHistory> findByFaq(Faq faq);

    // FAQ ID로 히스토리 조회 (필요 시)
    List<FaqHistory> findByFaqId(Long faqId);

    // 최신 수정 이력 하나만 조회 (ex. 최근 변경 로그)
    FaqHistory findTopByFaqIdOrderByEditedAtDesc(Long faqId);
}
