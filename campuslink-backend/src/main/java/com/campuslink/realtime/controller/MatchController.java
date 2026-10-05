package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.response.MatchResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Matchs", description = "Gestion des mises en relation mutuelles")
public class MatchController {

    private final MatchService matchService;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    @Operation(summary = "Lister mes matchs actifs")
    public ResponseEntity<List<MatchResponse>> mesMatchs() {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.ok(matchService.listerMatchsActifs(userId));
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique complet de mes matchs (actifs + rompus)")
    public ResponseEntity<List<MatchResponse>> historique() {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.ok(matchService.listerHistorique(userId));
    }

    @DeleteMapping("/{matchId}")
    @Operation(summary = "Rompre un match (unmatch)")
    public ResponseEntity<Void> unmatch(@PathVariable Long matchId) {
        Long userId = currentUserPort.getCurrentUserId();
        matchService.unmatch(userId, matchId);
        return ResponseEntity.noContent().build();
    }
}
