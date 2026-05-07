package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqFile;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FaqFileRepository extends JpaRepository<FaqFile, Long> {
    List<FaqFile> findByUrlIn(List<String> urls);

    @Query("""
    select ff
    from FaqFile ff
    where ff.faq is null
      and ff.createdAt < :time
      and ff.storagePath not in (
          select hf.storagePath
          from FaqHistoryFile hf
      )
""")
    List<FaqFile> findOrphanFiles(LocalDateTime time);
}