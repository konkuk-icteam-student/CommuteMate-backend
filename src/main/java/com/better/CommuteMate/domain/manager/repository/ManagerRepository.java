package com.better.CommuteMate.domain.manager.repository;

import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByNameAndOrganizationAndPhonenum(String name, Organization organization, String phonenum);
    boolean existsByOrganizationId(Long organizationId);
}
