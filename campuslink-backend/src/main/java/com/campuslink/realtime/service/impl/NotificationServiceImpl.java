package com.campuslink.realtime.service.impl;

import com.campuslink.realtime.dto.response.NotificationResponse;
import com.campuslink.realtime.entity.Notification;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.integration.FcmPushPort;
import com.campuslink.realtime.repository.NotificationRepository;
import com.campuslink.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmPushPort fcmPushPort;

    @Override
    @Transactional
    public NotificationResponse creerEtEnvoyer(Long userId, NotificationType type, String contenu, Long referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .contenu(contenu)
                .referenceId(referenceId)
                .lu(false)
                .build();
        notification = notificationRepository.save(notification);

        NotificationResponse response = toResponse(notification);

        // Diffusion temps reel vers /user/{userId}/queue/notifications
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", response);

        // Preparation mobile (no-op en V1 web)
        fcmPushPort.envoyerPush(userId, type.name(), contenu, referenceId);

        return response;
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
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .contenu(n.getContenu())
                .referenceId(n.getReferenceId())
                .lu(n.isLu())
                .dateCreation(n.getDateCreation())
                .build();
    }
}
