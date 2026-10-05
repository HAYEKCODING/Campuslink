package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.response.DashboardStatsResponse;
import com.campuslink.realtime.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@Tag(name = "Dashboard", description = "Statistiques d'administration (matchs, messages, signalements)")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Statistiques globales pour le tableau de bord admin")
    public ResponseEntity<DashboardStatsResponse> getStatistiques() {
        return ResponseEntity.ok(dashboardService.getStatistiques());
    }
}
