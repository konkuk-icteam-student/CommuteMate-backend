package com.better.CommuteMate.domain.team.repository;

import com.better.CommuteMate.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>  {
    boolean existsByName(String name);
}
