package com.campuslink.controller;

import com.campuslink.dto.request.ContactRequest;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.PublicStatsResponse;
import com.campuslink.dto.response.TestimonialResponse;
import com.campuslink.service.PublicContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints publics de la landing page — statistiques, témoignages,
 * contact et référentiels de suggestions.
 *
 * <p>Ces routes sont les seules (avec l'authentification, les profils
 * publics et Swagger) listées dans {@code SecurityConstants.PUBLIC_ENDPOINTS} :
 * elles ne retournent aucune donnée personnelle et doivent le rester —
 * un visiteur non connecté doit pouvoir afficher la page d'accueil sans
 * être redirigé vers la connexion.</p>
 */
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Contenu public", description = "Endpoints publics de la landing page (statistiques, témoignages, contact, référentiels)")
public class PublicContentController {

    private final PublicContentService publicContentService;

    @GetMapping("/stats/public")
    @Operation(summary = "Statistiques publiques (compteur d'inscrits)", description = "Endpoint public.")
    public ApiResponse<PublicStatsResponse> getPublicStats() {
        return ApiResponse.success(publicContentService.getPublicStats());
    }

    @GetMapping("/testimonials")
    @Operation(summary = "Témoignages affichés sur la landing page", description = "Endpoint public.")
    public ApiResponse<List<TestimonialResponse>> getTestimonials() {
        return ApiResponse.success(publicContentService.getTestimonials());
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Déposer un message via le formulaire de contact", description = "Endpoint public : le message est mis en file d'attente côté serveur.")
    public ApiResponse<Void> submitContact(@Valid @RequestBody ContactRequest request) {
        publicContentService.submitContact(request);
        return ApiResponse.success(null, "Message envoyé, nous vous répondrons au plus vite.");
    }

    @GetMapping("/reference/{type}")
    @Operation(
            summary = "Valeurs d'un référentiel de suggestions",
            description = "Endpoint public. Types : universities, faculties, neighborhoods, interests — déduits des profils en base."
    )
    public ApiResponse<List<String>> getReferenceValues(
            @Parameter(description = "Type de référentiel") @PathVariable String type) {
        return ApiResponse.success(publicContentService.findReferenceValues(type));
    }

}
