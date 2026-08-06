package com.better.CommuteMate.domain.workplace.repository;

import com.better.CommuteMate.domain.workplace.entity.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkplaceRepository extends JpaRepository<Workplace, String> {

    Optional<Workplace> findFirstByOrganizationId(String organizationId);
}