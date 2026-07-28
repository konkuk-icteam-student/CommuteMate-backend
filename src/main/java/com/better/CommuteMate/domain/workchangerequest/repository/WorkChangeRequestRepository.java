package com.better.CommuteMate.domain.workchangerequest.repository;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface WorkChangeRequestRepository extends JpaRepository<WorkChangeRequest, Long> {
    List<WorkChangeRequest> findByUser_UserId(Long userId);

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
