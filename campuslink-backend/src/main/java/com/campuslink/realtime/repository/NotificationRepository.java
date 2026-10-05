package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByDateCreationDesc(Long userId);

    List<Notification> findAllByUserIdAndLuFalseOrderByDateCreationDesc(Long userId);

    long countByUserIdAndLuFalse(Long userId);
}
