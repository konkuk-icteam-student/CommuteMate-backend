package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqFileRepository extends JpaRepository<FaqFile, Long> {
}