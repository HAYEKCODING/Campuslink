package com.campuslink.realtime.controller;

import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.dto.request.MessageRequest;
import com.campuslink.realtime.dto.request.TypingEvent;
import com.campuslink.realtime.dto.response.TypingNotification;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.service.MatchService;
import com.campuslink.realtime.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Points d'entree STOMP (client -> serveur, prefixe /app configure dans WebSocketConfig).
 *
 * Envoi d'un message   : client publie sur /app/chat.send      -> le serveur persiste et
 *                         redistribue via convertAndSendToUser (voir MessageServiceImpl).
 * Indicateur de saisie : client publie sur /app/chat.typing     -> diffuse en broadcast sur
 *                         /topic/matches/{matchId}/typing (pas de persistance, ephemere).
 *                         Le frontend est responsable du timeout d'affichage (ex: 3-5s sans
 *                         nouvel evenement => on cache l'indicateur), evitant un job serveur dedie.
 *
 * Le Principal (id utilisateur) est celui attache lors du handshake par
 * StompAuthChannelInterceptor.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void envoyerMessage(@Valid @Payload MessageRequest request, Principal principal) {
        Long expediteurId = Long.valueOf(principal.getName());
        messageService.envoyer(expediteurId, request.getMatchId(), request.getContenu());
    }

    @MessageMapping("/chat.typing")
    public void diffuserTyping(TypingEvent event, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        Match match = matchService.getMatchOuException(event.getMatchId());
        matchService.verifierParticipant(match, userId);
        if (match.getStatut() != MatchStatus.ACTIF) {
            throw new DuplicateResourceException("Ce match n'est plus actif : messagerie indisponible");
        }
        TypingNotification notification = TypingNotification.builder()
                .matchId(event.getMatchId())
                .utilisateurId(userId)
                .enTrainDecrire(event.isEnTrainDecrire())
                .build();
        messagingTemplate.convertAndSend("/topic/matches/" + event.getMatchId() + "/typing", notification);
    }
}
