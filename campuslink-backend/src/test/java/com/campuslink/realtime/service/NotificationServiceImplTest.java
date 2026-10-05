package com.campuslink.realtime.service;

import com.campuslink.realtime.entity.Notification;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.realtime.integration.FcmPushPort;
import com.campuslink.realtime.repository.NotificationRepository;
import com.campuslink.realtime.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private FcmPushPort fcmPushPort;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void creerEtEnvoyer_persisteDiffuseEtPreparELeFcm() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.creerEtEnvoyer(5L, NotificationType.MATCH, "Nouveau match !", 42L);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("5"), eq("/queue/notifications"), any());
        verify(fcmPushPort).envoyerPush(eq(5L), eq("MATCH"), eq("Nouveau match !"), eq(42L));
    }

    @Test
    void marquerCommeLue_neModifiePasSiUtilisateurNeCorrespondPas() {
        Notification notification = Notification.builder().id(1L).userId(999L).lu(false).build();
        when(notificationRepository.findById(1L)).thenReturn(java.util.Optional.of(notification));

        try {
            notificationService.marquerCommeLue(5L, 1L);
        } catch (Exception ignored) {
            // attendu : ResourceNotFoundException car la notification n'appartient pas a l'utilisateur 5
        }

        assertThat(notification.isLu()).isFalse();
    }
}
