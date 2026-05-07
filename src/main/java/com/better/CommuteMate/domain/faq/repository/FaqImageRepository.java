package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqImage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FaqImageRepository extends JpaRepository<FaqImage, Long> {
    List<FaqImage> findByUrlIn(List<String> urls);

    @Query("""
    select fi
    from FaqImage fi
    where fi.faq is null
      and fi.createdAt < :time
      and fi.storagePath not in (
          select hi.storagePath
          from FaqHistoryImage hi
      )
""")
    List<FaqImage> findOrphanImages(LocalDateTime time);
}