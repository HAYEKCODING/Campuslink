package com.campuslink.realtime.service;

import com.campuslink.realtime.dto.response.NotificationResponse;
import com.campuslink.realtime.entity.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    /** Cree une notification, la persiste et la pousse en temps reel (WebSocket + placeholder FCM). */
    NotificationResponse creerEtEnvoyer(Long userId, NotificationType type, String contenu, Long referenceId);

    List<NotificationResponse> listerPourUtilisateur(Long userId);

    List<NotificationResponse> listerNonLues(Long userId);

    long compterNonLues(Long userId);

    void marquerCommeLue(Long userId, Long notificationId);

    void marquerToutesCommeLues(Long userId);
}
