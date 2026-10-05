package com.campuslink.service;

import com.campuslink.entity.User;
import com.campuslink.enums.OtpType;

/**
 * Envoi d'emails transactionnels liés à la sécurité du compte (codes OTP,
 * liens de réinitialisation de mot de passe).
 *
 * <p>Interface volontairement restreinte à ce périmètre (SRP) — un futur
 * besoin d'emails non liés à la sécurité (newsletter, notifications produit)
 * justifierait un service distinct plutôt que d'élargir celui-ci.</p>
 */
public interface EmailService {

    /**
     * Envoie un email contenant un code OTP en clair à l'utilisateur.
     *
     * @throws com.campuslink.exception.EmailDeliveryException si l'envoi échoue
     *         (serveur SMTP injoignable, etc.)
     */
    void sendOtpCode(User user, String code, OtpType type, int expirationMinutes);

    /**
     * Envoie un email contenant un lien de réinitialisation de mot de passe.
     *
     * @param resetLink URL complète (token déjà inclus en paramètre de requête)
     * @throws com.campuslink.exception.EmailDeliveryException si l'envoi échoue
     */
    void sendPasswordResetEmail(User user, String resetLink, int expirationMinutes);

}
