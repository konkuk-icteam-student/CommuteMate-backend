package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqHistoryFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqHistoryFileRepository
        extends JpaRepository<FaqHistoryFile, Long> {
}