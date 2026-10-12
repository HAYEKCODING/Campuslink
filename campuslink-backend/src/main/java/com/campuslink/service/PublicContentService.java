package com.campuslink.service;

import com.campuslink.dto.request.ContactRequest;
import com.campuslink.dto.response.PublicStatsResponse;
import com.campuslink.dto.response.TestimonialResponse;

import java.util.List;

/**
 * Contenu public de la landing page : statistiques, témoignages,
 * formulaire de contact et référentiels de suggestions.
 *
 * <p>Toutes les méthodes sont appelées sans authentification (routes
 * listées dans {@code SecurityConstants.PUBLIC_ENDPOINTS}) — elles ne
 * doivent donc exposer que des données publiques.</p>
 */
public interface PublicContentService {

    /** Compteur d'inscrits + quelques avatars réels pour la landing. */
    PublicStatsResponse getPublicStats();

    /** Témoignages actifs, triés pour l'affichage. */
    List<TestimonialResponse> getTestimonials();

    /**
     * Enregistre un message du formulaire de contact (file d'attente
     * persistée, voir {@code ContactMessage}).
     */
    void submitContact(ContactRequest request);

    /**
     * Valeurs d'un référentiel de suggestions ({@code universities},
     * {@code faculties}, {@code neighborhoods}, {@code interests}),
     * déduites des profils réellement présents en base.
     *
     * @throws com.campuslink.exception.BadRequestException si le type est inconnu
     */
    List<String> findReferenceValues(String referenceType);

}
