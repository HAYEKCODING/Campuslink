package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.NotificationResponse;
import com.campuslink.realtime.entity.Notification;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.integration.FcmPushPort;
import com.campuslink.realtime.repository.NotificationRepository;
import com.campuslink.realtime.service.NotificationService;
import com.campuslink.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmPushPort fcmPushPort;

    @Override
    @Transactional
    public NotificationResponse creerEtEnvoyer(Long userId, NotificationType type, String contenu, Long referenceId) {
        boolean sendAlert = profileRepository.findByUser_LegacyId(userId)
                .map(profile -> notificationsEnabled(profile, type))
                .orElse(true);

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .contenu(contenu)
                .referenceId(referenceId)
                .lu(false)
                .build();
        notification = notificationRepository.save(notification);

        NotificationResponse response = toResponse(notification);

        afterCommit(() -> {
            if (sendAlert) {
                messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", response);
                fcmPushPort.envoyerPush(userId, type.name(), contenu, referenceId);
            }
        });

        return response;
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private boolean notificationsEnabled(com.campuslink.entity.Profile profile, NotificationType type) {
        return switch (type) {
            case LIKE -> profile.isNotificationLikes();
            case MATCH -> profile.isNotificationMatches();
            case MESSAGE -> profile.isNotificationMessages();
            case APPEL -> profile.isNotificationMessages();
            case SIGNALEMENT -> profile.isNotificationReports();
        };
    }

    @Override
    public List<NotificationResponse> listerPourUtilisateur(Long userId) {
        return notificationRepository.findAllByUserIdOrderByDateCreationDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> listerNonLues(Long userId) {
        return notificationRepository.findAllByUserIdAndLuFalseOrderByDateCreationDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long compterNonLues(Long userId) {
        return notificationRepository.countByUserIdAndLuFalse(userId);
    }

    @Override
    @Transactional
    public void marquerCommeLue(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification introuvable : " + notificationId);
        }
        notification.setLu(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void marquerToutesCommeLues(Long userId) {
        notificationRepository.findAllByUserIdAndLuFalseOrderByDateCreationDesc(userId)
                .forEach(n -> n.setLu(true));
    }

    private NotificationResponse toResponse(Notification n) {
        String navigationUrl = switch (n.getType()) {
            case LIKE -> n.getReferenceId() == null ? null
                    : profileRepository.findByUser_LegacyId(n.getReferenceId())
                            .map(profile -> "/app/profil/" + profile.getId())
                            .orElse(null);
            case MATCH, MESSAGE, APPEL -> n.getReferenceId() == null ? null
                    : "/app/messages/" + n.getReferenceId();
            case SIGNALEMENT -> null;
        };
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .contenu(n.getContenu())
                .referenceId(n.getReferenceId())
                .navigationUrl(navigationUrl)
                .lu(n.isLu())
                .dateCreation(n.getDateCreation())
                .build();
    }
}
