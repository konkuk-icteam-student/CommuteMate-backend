package com.better.CommuteMate.domain.category.repository;

import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerCategoryRepository extends JpaRepository<ManagerCategory, Long> {
    boolean existsByManagerIdAndCategoryId(Long managerId, Long categoryId);
    boolean existsByCategoryId(Long categoryId);
    void deleteByManager(Manager manager);
    Optional<ManagerCategory>  findByManagerIdAndCategoryId(Long managerId, Long categoryId);

    @Query("""
    select distinct mc.category.id
    from ManagerCategory mc
    where mc.manager.organization.id = :organizationId
    """)
    List<Long> findCategoryIdsByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
    select mc
    from ManagerCategory mc
    where (:categoryId is null or mc.category.id = :categoryId)
      and (:organization is null or mc.manager.organization = :organization)
      and (:favoriteOnly = false or mc.favorite = true)
      and (
          :searchName is null\s
          or length(trim(:searchName)) = 0
          or lower(mc.manager.name) like lower(concat('%', :searchName, '%'))
      )
    """)
    List<ManagerCategory> getManagers(
            @Param("categoryId") Long categoryId,
            @Param("organization") Organization organization,
            @Param("favoriteOnly") boolean favoriteOnly,
            @Param("searchName") String searchName
    );
}