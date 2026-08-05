package com.better.CommuteMate.domain.user.repository;

import com.better.CommuteMate.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    List<UserProfile> findAllByUserIdIn(Collection<Long> userIds);
}
