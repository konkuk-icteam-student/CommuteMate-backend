package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByDeletedFlagFalse();
}
