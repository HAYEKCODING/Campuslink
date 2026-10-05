package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.request.LikeRequest;
import com.campuslink.realtime.dto.response.LikeResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Gestion des likes entre utilisateurs")
public class LikeController {

    private final LikeService likeService;
    private final CurrentUserPort currentUserPort;

    @PostMapping
    @Operation(summary = "Liker un profil (detecte automatiquement un match mutuel)")
    public ResponseEntity<LikeResponse> liker(@Valid @RequestBody LikeRequest request) {
        Long userId = currentUserPort.getCurrentUserId();
        LikeResponse response = likeService.liker(userId, request.getCibleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{cibleId}")
    @Operation(summary = "Retirer un like (avant qu'il y ait match)")
    public ResponseEntity<Void> retirerLike(@PathVariable Long cibleId) {
        Long userId = currentUserPort.getCurrentUserId();
        likeService.retirerLike(userId, cibleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lister les likes envoyes par l'utilisateur connecte")
    public ResponseEntity<List<LikeResponse>> mesLikes() {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.ok(likeService.listerMesLikes(userId));
    }

    @GetMapping("/recus")
    @Operation(summary = "Lister les likes recus par l'utilisateur connecte")
    public ResponseEntity<List<LikeResponse>> likesRecus() {
        Long userId = currentUserPort.getCurrentUserId();
        return ResponseEntity.ok(likeService.listerLikesRecus(userId));
    }
}
