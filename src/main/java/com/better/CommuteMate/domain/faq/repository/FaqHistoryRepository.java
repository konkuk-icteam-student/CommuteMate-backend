package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FaqHistoryRepository extends JpaRepository<FaqHistory, Long> {

    void deleteByFaqIdAndEditedAt(Long faqId, LocalDate editedAt);

    Optional<FaqHistory> findByFaqIdAndEditedAt(Long faqId, LocalDate editedAt);

    @Query("""
        select fh.editedAt
        from FaqHistory fh
        where fh.faq.id = :faqId
        order by fh.editedAt desc
    """)
    List<LocalDate> findAllEditedDatesByFaqId(@Param("faqId") Long faqId);
}
