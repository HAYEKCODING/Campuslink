package com.campuslink.service;

/**
 * Contrat métier du module Forgot Password.
 */
public interface PasswordResetService {

    /**
     * Génère un lien de réinitialisation et l'envoie par email si l'adresse
     * correspond à un compte existant.
     *
     * <p>Ne révèle jamais si l'email existe : aucune exception n'est levée
     * pour un email inconnu, le comportement observable est identique à un
     * succès (protection contre l'énumération d'emails).</p>
     *
     * @throws com.campuslink.exception.TooManyRequestsException si une demande
     *         a déjà été effectuée récemment pour ce compte (délai anti-spam)
     */
    void requestReset(String email);

    /**
     * Consomme un token de réinitialisation valide pour définir un nouveau
     * mot de passe (hashé avec BCrypt), puis révoque toutes les sessions
     * actives de l'utilisateur.
     *
     * @throws com.campuslink.exception.InvalidTokenException si le token est
     *         invalide, déjà utilisé ou expiré
     */
    void resetPassword(String token, String newPassword);

}
