package com.better.CommuteMate.domain.handovermemo.repository;

import com.better.CommuteMate.domain.handovermemo.entity.HandoverMemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HandoverMemoRepository extends JpaRepository<HandoverMemo, Long> {

    @Query("""
            select memo
            from HandoverMemo memo
            join fetch memo.createdBy
            where memo.organizationId = :organizationId
              and memo.createdAt >= :startAt
              and memo.createdAt < :endAt
            order by memo.createdAt asc, memo.memoId asc
            """)
    List<HandoverMemo> findDailyMemos(
            @Param("organizationId") Long organizationId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
