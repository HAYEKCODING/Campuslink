package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.request.ReportRequest;
import com.campuslink.realtime.dto.request.ReportStatusUpdateRequest;
import com.campuslink.realtime.dto.response.ReportResponse;
import com.campuslink.realtime.entity.enums.ReportStatus;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Signalements", description = "Signalement de profils/conversations et traitement par la modération")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserPort currentUserPort;

    @PostMapping
    @Operation(summary = "Signaler un profil ou une conversation")
    public ResponseEntity<ReportResponse> creer(@Valid @RequestBody ReportRequest request) {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.creer(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Lister les signalements (filtrable par statut) - MODERATOR/ADMIN")
    public ResponseEntity<List<ReportResponse>> lister(@RequestParam(required = false) ReportStatus statut) {
        return ResponseEntity.ok(reportService.lister(statut));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Changer le statut d'un signalement - MODERATOR/ADMIN")
    public ResponseEntity<ReportResponse> changerStatut(@PathVariable Long id, @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(reportService.mettreAJourStatut(id, request.getStatut()));
    }

    @PostMapping("/{id}/archiver")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Archiver un signalement traité - MODERATOR/ADMIN")
    public ResponseEntity<Void> archiver(@PathVariable Long id) {
        reportService.archiver(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un signalement - ADMIN uniquement")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        reportService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
