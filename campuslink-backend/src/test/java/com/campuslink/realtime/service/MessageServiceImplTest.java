package com.campuslink.realtime.service;

import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.Message;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.repository.MessageRepository;
import com.campuslink.realtime.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

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

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void envoyer_persisteEtDiffuseAuDestinataire() {
        Match match = Match.builder().id(10L).utilisateur1Id(1L).utilisateur2Id(2L).statut(MatchStatus.ACTIF).build();
        when(matchService.getMatchOuException(10L)).thenReturn(match);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(500L);
            return m;
        });

        messageService.envoyer(1L, 10L, "Salut !");

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
