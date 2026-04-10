package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqImageRepository extends JpaRepository<FaqImage, Long> {
    List<FaqImage> findByUrlIn(List<String> urls);
}