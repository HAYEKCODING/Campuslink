package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.request.MessageRequest;
import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matches/{matchId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messagerie", description = "Historique et lecture des messages (l'envoi temps reel passe par WebSocket /app/chat.send)")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    @Operation(summary = "Historique paginé d'une conversation")
    public ResponseEntity<Page<MessageResponse>> historique(
            @PathVariable Long matchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = currentUserPort.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(messageService.getHistorique(userId, matchId, pageable));
    }

    /**
     * Envoi d'un message via HTTP.
     *
     * <p>Équivalent direct du point d'entrée STOMP {@code /app/chat.send} (mêmes
     * règles métier : participant au match, match actif) : il permet aux clients
     * sans client WebSocket (frontend web actuel, intégrations tierces) d'envoyer
     * un message sans ouvrir de connexion temps réel. Les destinataires restent
     * notifiés en temps réel via {@code /user/queue/messages}.</p>
     */
    @PostMapping
    @Operation(summary = "Envoyer un message dans un match (repli HTTP de /app/chat.send)")
    public ResponseEntity<MessageResponse> envoyer(@PathVariable Long matchId,
                                                   @Valid @RequestBody MessageRequest request) {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.ok(messageService.envoyer(userId, matchId, request.getContenu()));
    }

    @PostMapping("/lu")
    @Operation(summary = "Marquer tous les messages recus de ce match comme lus")
    public ResponseEntity<Void> marquerCommeLu(@PathVariable Long matchId) {
        Long userId = currentUserPort.getCurrentUserId();
        messageService.marquerCommeLu(userId, matchId);
        return ResponseEntity.noContent().build();
    }
}
