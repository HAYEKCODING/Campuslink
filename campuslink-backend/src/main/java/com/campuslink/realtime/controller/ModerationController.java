package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.request.ModerationActionRequest;
import com.campuslink.realtime.entity.ModerationLog;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@Tag(name = "Moderation", description = "Actions de moderation : avertissement, suspension, bannissement")
public class ModerationController {

    private final ModerationService moderationService;
    private final CurrentUserPort currentUserPort;

    @PostMapping("/avertir")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Avertir un utilisateur (trace uniquement, pas de changement de statut)")
    public ResponseEntity<ModerationLog> avertir(@Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(moderationService.avertir(currentUserPort.getCurrentUserId(), request));
    }

    @PostMapping("/suspendre")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Suspendre temporairement un compte")
    public ResponseEntity<ModerationLog> suspendre(@Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(moderationService.suspendre(currentUserPort.getCurrentUserId(), request));
    }

    @PostMapping("/bannir")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bannir definitivement un compte - ADMIN uniquement")
    public ResponseEntity<ModerationLog> bannir(@Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(moderationService.bannir(currentUserPort.getCurrentUserId(), request));
    }

    @PostMapping("/debannir")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lever un bannissement - ADMIN uniquement")
    public ResponseEntity<ModerationLog> debannir(@Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(moderationService.debannir(currentUserPort.getCurrentUserId(), request));
    }

    @GetMapping("/historique/{utilisateurId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Historique de moderation pour un utilisateur donne")
    public ResponseEntity<List<ModerationLog>> historique(@PathVariable Long utilisateurId) {
        return ResponseEntity.ok(moderationService.historiquePourUtilisateur(utilisateurId));
    }

    @GetMapping("/journal")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Journal complet des actions de moderation")
    public ResponseEntity<List<ModerationLog>> journal() {
        return ResponseEntity.ok(moderationService.journalComplet());
    }
}
