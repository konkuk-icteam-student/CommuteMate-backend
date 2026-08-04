package com.better.CommuteMate.domain.notification.repository;

import com.better.CommuteMate.domain.notification.entity.NotificationCheckState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationCheckStateRepository extends JpaRepository<NotificationCheckState, Long> {
}
