package com.campuslink.realtime.service;

import com.campuslink.realtime.entity.Notification;
import com.campuslink.realtime.entity.enums.NotificationType;
import com.campuslink.realtime.integration.FcmPushPort;
import com.campuslink.realtime.repository.NotificationRepository;
import com.campuslink.realtime.service.impl.NotificationServiceImpl;
import com.campuslink.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.campuslink.exception.ResourceNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock
    private ProfileRepository profileRepository;

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
    void creerEtEnvoyer_conserveDansLeCentreMaisNeDiffusePasSiCategorieDesactivee() {
        com.campuslink.entity.Profile profile = com.campuslink.entity.Profile.builder()
                .firstName("Awa")
                .lastName("Kone")
                .notificationMatches(false)
                .build();
        when(profileRepository.findByUser_LegacyId(5L)).thenReturn(java.util.Optional.of(profile));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.creerEtEnvoyer(5L, NotificationType.MATCH, "Nouveau match !", 42L);

        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(messagingTemplate, fcmPushPort);
    }

    @Test
    void listerPourUtilisateur_fournitUneCibleDeConversationPourMessageEtMatch() {
        Notification notification = Notification.builder()
                .id(12L)
                .userId(5L)
                .type(NotificationType.MESSAGE)
                .referenceId(42L)
                .contenu("Nouveau message")
                .lu(false)
                .build();
        when(notificationRepository.findAllByUserIdOrderByDateCreationDesc(5L))
                .thenReturn(List.of(notification));

        var listed = notificationService.listerPourUtilisateur(5L);

        assertThat(listed).singleElement()
                .satisfies(item -> assertThat(item.getNavigationUrl()).isEqualTo("/app/messages/42"));
    }

    @Test
    void marquerCommeLue_neModifiePasSiUtilisateurNeCorrespondPas() {
        Notification notification = Notification.builder().id(1L).userId(999L).lu(false).build();
        when(notificationRepository.findById(1L)).thenReturn(java.util.Optional.of(notification));

        assertThrows(ResourceNotFoundException.class, () -> notificationService.marquerCommeLue(5L, 1L));

        assertThat(notification.isLu()).isFalse();
    }
}
