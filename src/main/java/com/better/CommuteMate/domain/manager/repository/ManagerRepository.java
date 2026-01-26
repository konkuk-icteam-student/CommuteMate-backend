package com.better.CommuteMate.domain.manager.repository;

import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByNameAndTeamAndPhonenum(String name, Team team, String phonenum);
}
