package com.better.CommuteMate.domain.workchangerequest.repository;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface WorkChangeRequestRepository extends JpaRepository<WorkChangeRequest, Long> {
    List<WorkChangeRequest> findByUser_UserId(Long userId);

    // 같은 요청이 동시에 승인·거절되는 것을 막기 위해 처리 완료 시점까지 행 잠금을 유지합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from WorkChangeRequest request
            join fetch request.user
            where request.requestId = :requestId
            """)
    java.util.Optional<WorkChangeRequest> findForProcessing(
            @Param("requestId") Long requestId
    );

    @Query(
            value = """
                    select distinct request
                    from WorkChangeRequest request
                    join fetch request.user user
                    where user.organizationId = :organizationId
                      and exists (
                        select item.itemId
                        from WorkChangeRequestItem item
                        where item.request = request
                          and item.date between :startDate and :endDate
                      )
                      and (:statusCode is null or request.statusCode = :statusCode)
                    """,
            countQuery = """
                    select count(distinct request.requestId)
                    from WorkChangeRequest request
                    where request.user.organizationId = :organizationId
                      and exists (
                        select item.itemId
                        from WorkChangeRequestItem item
                        where item.request = request
                          and item.date between :startDate and :endDate
                      )
                      and (:statusCode is null or request.statusCode = :statusCode)
                    """
    )
    Page<WorkChangeRequest> findAdminRequests(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statusCode") CodeType statusCode,
            Pageable pageable
    );

    @Query("""
            select count(distinct request.requestId)
            from WorkChangeRequest request
            where request.user.organizationId = :organizationId
              and exists (
                select item.itemId
                from WorkChangeRequestItem item
                where item.request = request
                  and item.date between :startDate and :endDate
              )
              and (:statusCode is null or request.statusCode = :statusCode)
            """)
    long countAdminRequests(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statusCode") CodeType statusCode
    );
}
