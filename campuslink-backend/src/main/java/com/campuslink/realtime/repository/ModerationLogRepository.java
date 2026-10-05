package com.campuslink.realtime.repository;

import com.campuslink.realtime.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    List<ModerationLog> findAllByUtilisateurCibleIdOrderByDateActionDesc(Long utilisateurCibleId);

    List<ModerationLog> findAllByOrderByDateActionDesc();
}
