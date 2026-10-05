package com.campuslink.service;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;

import java.util.UUID;

/**
 * Contrat métier du module Profile.
 */
public interface ProfileService {

    /**
     * Crée le profil de l'utilisateur donné.
     *
     * @throws com.campuslink.exception.DuplicateResourceException si un profil existe déjà
     * @throws com.campuslink.exception.ResourceNotFoundException si l'utilisateur n'existe pas
     */
    ProfileResponse createProfile(UUID userId, ProfileRequest request);

    /**
     * Remplace intégralement le profil existant de l'utilisateur donné.
     *
     * @throws com.campuslink.exception.ResourceNotFoundException si aucun profil n'existe
     */
    ProfileResponse updateProfile(UUID userId, ProfileRequest request);

    /**
     * Supprime le profil de l'utilisateur donné.
     *
     * @throws com.campuslink.exception.ResourceNotFoundException si aucun profil n'existe
     */
    void deleteProfile(UUID userId);

    /**
     * Retourne le profil de l'utilisateur authentifié.
     *
     * @throws com.campuslink.exception.ResourceNotFoundException si aucun profil n'existe
     */
    ProfileResponse getOwnProfile(UUID userId);

    /**
     * Retourne un profil par son identifiant, pour consultation publique.
     *
     * @throws com.campuslink.exception.ResourceNotFoundException si le profil n'existe pas
     */
    ProfileResponse getPublicProfile(UUID profileId);

}
