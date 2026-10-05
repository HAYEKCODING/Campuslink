package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.response.NotificationResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Centre de notifications (like, match, message, signalement)")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    @Operation(summary = "Lister toutes mes notifications")
    public ResponseEntity<List<NotificationResponse>> lister() {
        return ResponseEntity.ok(notificationService.listerPourUtilisateur(currentUserPort.getCurrentUserId()));
    }

    @GetMapping("/non-lues")
    @Operation(summary = "Lister mes notifications non lues")
    public ResponseEntity<List<NotificationResponse>> nonLues() {
        return ResponseEntity.ok(notificationService.listerNonLues(currentUserPort.getCurrentUserId()));
    }

    @GetMapping("/compteur")
    @Operation(summary = "Nombre de notifications non lues (pour badge UI)")
    public ResponseEntity<Map<String, Long>> compteur() {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.compterNonLues(currentUserPort.getCurrentUserId())));
    }

    @PostMapping("/{id}/lu")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> marquerLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(currentUserPort.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lu-tout")
    @Operation(summary = "Marquer toutes mes notifications comme lues")
    public ResponseEntity<Void> marquerToutesLues() {
        notificationService.marquerToutesCommeLues(currentUserPort.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
