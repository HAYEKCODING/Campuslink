package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.response.ChatMediaDownload;
import com.campuslink.realtime.dto.response.ChatMediaLibraryItemResponse;
import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.exception.BadRequestException;
import com.campuslink.realtime.entity.enums.ChatMediaCategory;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/matches/{matchId}/media")
@RequiredArgsConstructor
@Tag(name = "Médias de conversation", description = "Partage privé et bibliothèque de pièces jointes")
public class ChatMediaController {
    private static final int MAX_PAGE_SIZE = 100;

    private final MessageService messageService;
    private final CurrentUserPort currentUserPort;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envoyer une pièce jointe dans une conversation")
    public ResponseEntity<MessageResponse> envoyer(@PathVariable Long matchId,
                                                   @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(messageService.envoyerMedia(
                currentUserPort.getCurrentUserId(), matchId, file));
    }

    @GetMapping
    @Operation(summary = "Consulter les médias, documents ou liens d'une conversation")
    public ResponseEntity<Page<ChatMediaLibraryItemResponse>> bibliotheque(
            @PathVariable Long matchId,
            @RequestParam ChatMediaCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Paramètres de pagination invalides.");
        }
        return ResponseEntity.ok(messageService.getMediaLibrary(
                currentUserPort.getCurrentUserId(), matchId, category, PageRequest.of(page, size)));
    }

    @GetMapping("/{messageId}/content")
    @Operation(summary = "Télécharger ou prévisualiser un média après vérification de la conversation")
    public ResponseEntity<Resource> contenu(@PathVariable Long matchId, @PathVariable Long messageId) {
        ChatMediaDownload media = messageService.getMedia(
                currentUserPort.getCurrentUserId(), matchId, messageId);
        boolean inline = media.getContentType().startsWith("image/")
                || media.getContentType().startsWith("video/")
                || "application/pdf".equals(media.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(media.getContentType()));
        headers.setContentLength(media.getSizeBytes());
        headers.setContentDisposition(ContentDisposition.builder(inline ? "inline" : "attachment")
                .filename(media.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok()
                .headers(headers)
                .cacheControl(CacheControl.noStore())
                .body(media.getResource());
    }
}
