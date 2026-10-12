package com.campuslink.realtime.service;

import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.Message;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.repository.MessageRepository;
import com.campuslink.realtime.service.impl.MessageServiceImpl;
import com.campuslink.config.ChatMediaProperties;
import com.campuslink.realtime.service.ChatMediaStorage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MatchService matchService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatMediaStorage chatMediaStorage;
    @Mock
    private ChatMediaProperties chatMediaProperties;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void envoyerMedia_persisteLesMetadonneesEtDiffuseAuDestinataire() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(chatMediaProperties.getMaxFileSizeBytes()).thenReturn(52_428_800L);
        when(chatMediaStorage.store(any(), eq(".jpg"))).thenReturn("c8a1d45e-8173-4f2b-bd99-4c8163ec2b98.jpg");
        when(messageRepository.saveAndFlush(any(Message.class))).thenAnswer(inv -> {
            Message message = inv.getArgument(0);
            message.setId(501L);
            return message;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

        var response = messageService.envoyerMedia(1L, 10L, file);

        assertThat(response.getMatchId()).isEqualTo(10L);
        assertThat(response.getExpediteurId()).isEqualTo(1L);
        assertThat(response.getMedia().getFileName()).isEqualTo("photo.jpg");
        assertThat(response.getMedia().getContentType()).isEqualTo("image/jpeg");
        verify(messageRepository).saveAndFlush(argThat(message ->
                message.getMatchId().equals(10L)
                        && message.getExpediteurId().equals(1L)
                        && message.getMediaStorageKey().endsWith(".jpg")));
        verify(notificationService).creerEtEnvoyer(eq(2L), any(), anyString(), eq(10L));
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/messages"), any());
    }

    @Test
    void envoyerMedia_refuseUnFichierDontLeMimeNeCorrespondPasAExtension() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(chatMediaProperties.getMaxFileSizeBytes()).thenReturn(52_428_800L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "application/pdf", "%PDF-1.7".getBytes());

        assertThrows(com.campuslink.exception.BadRequestException.class,
                () -> messageService.envoyerMedia(1L, 10L, file));
        verifyNoInteractions(chatMediaStorage);
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMedia_verifieLeMatchEtNeRetourneQueLeFichierAssocie() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        Message message = Message.builder()
                .id(501L)
                .matchId(10L)
                .expediteurId(1L)
                .mediaStorageKey("c8a1d45e-8173-4f2b-bd99-4c8163ec2b98.jpg")
                .mediaOriginalName("photo.jpg")
                .mediaContentType("image/jpeg")
                .mediaSizeBytes(3L)
                .build();
        var resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(messageRepository.findByIdAndMatchId(501L, 10L)).thenReturn(Optional.of(message));
        when(chatMediaStorage.load(message.getMediaStorageKey())).thenReturn(Optional.of(resource));

        var result = messageService.getMedia(2L, 10L, 501L);

        assertThat(result.getFileName()).isEqualTo("photo.jpg");
        assertThat(result.getResource()).isSameAs(resource);
        verify(matchService).verifierParticipant(match, 2L);
        verify(messageRepository).findByIdAndMatchId(501L, 10L);
    }

    @Test
    void getMedia_refuseUnUtilisateurQuiNestPasParticipant() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        doThrow(new ForbiddenException("non participant")).when(matchService).verifierParticipant(match, 99L);

        assertThrows(ForbiddenException.class, () -> messageService.getMedia(99L, 10L, 501L));
        verifyNoInteractions(messageRepository, chatMediaStorage);
    }

    @Test
    void envoyer_persisteEtDiffuseAuDestinataire() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(500L);
            return m;
        });

        var response = messageService.envoyer(1L, 10L, "Salut 👋");

        assertThat(response.getMatchId()).isEqualTo(10L);
        assertThat(response.getExpediteurId()).isEqualTo(1L);
        assertThat(response.getContenu()).isEqualTo("Salut 👋");
        var persisted = org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(persisted.capture());
        assertThat(persisted.getValue().getMatchId()).isEqualTo(10L);
        assertThat(persisted.getValue().getExpediteurId()).isEqualTo(1L);
        assertThat(persisted.getValue().getContenu()).isEqualTo("Salut 👋");
        verify(matchService).verifierParticipant(match, 1L);
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/messages"), any());
        verify(notificationService).creerEtEnvoyer(eq(2L), any(), anyString(), eq(10L));
    }

    @Test
    void envoyer_refuseSiMatchRompu() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ROMPU).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);

        assertThrows(DuplicateResourceException.class, () -> messageService.envoyer(1L, 10L, "Salut"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void envoyer_refuseSiNonParticipant() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        doThrow(new ForbiddenException("non participant")).when(matchService).verifierParticipant(match, 99L);

        assertThrows(ForbiddenException.class, () -> messageService.envoyer(99L, 10L, "Salut"));
    }

    @Test
    void marquerCommeLu_diffuseReadReceiptSiMisesAJour() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(messageRepository.marquerCommeLu(eq(10L), eq(2L), any(Instant.class))).thenReturn(3);

        messageService.marquerCommeLu(2L, 10L);

        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/read-receipts"), any());
    }

    @Test
    void marquerCommeLu_neDiffusePasSiAucunMessageMisAJour() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(messageRepository.marquerCommeLu(eq(10L), eq(2L), any(Instant.class))).thenReturn(0);

        messageService.marquerCommeLu(2L, 10L);

        verifyNoInteractions(messagingTemplate);
    }
}
