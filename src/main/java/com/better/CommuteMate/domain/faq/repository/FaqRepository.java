package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long>, FaqQueryRepository {

    // 제목에 특정 키워드가 포함된 FAQ 검색 (부분 일치)
    List<Faq> findByTitleContaining(String keyword);

    // 작성자 이름 기준으로 FAQ 조회
    List<Faq> findByWriterName(String writerName);

    // 삭제되지 않은 FAQ만 조회
    List<Faq> findByDeletedFlagFalse();

    long countByWriter_UserIdAndStatusAndDeletedFlagFalse(
            Long userId,
            FaqStatus status
    );

    Page<Faq> findByWriter_UserIdAndStatusAndDeletedFlagFalse(
            Long userId,
            FaqStatus status,
            Pageable pageable
    );

    Page<Faq> findByWriter_UserIdAndStatusAndDeletedFlagFalseAndTitleContainingIgnoreCase(
            Long userId,
            FaqStatus status,
            String keyword,
            Pageable pageable
    );
}