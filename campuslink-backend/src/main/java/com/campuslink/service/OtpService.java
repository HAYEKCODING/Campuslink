package com.campuslink.service;

import com.campuslink.dto.response.OtpResponse;
import com.campuslink.enums.OtpType;

/**
 * Contrat métier du module OTP : génération/envoi et vérification de codes
 * à usage unique.
 */
public interface OtpService {

    /**
     * Génère un nouveau code OTP et l'envoie par email, en remplacement de tout
     * code non utilisé déjà émis pour ce couple (utilisateur, type).
     *
     * <p>Opération volontairement identique qu'il s'agisse d'un premier envoi ou
     * d'un renvoi — voir {@code OtpController} : {@code /send} et {@code /resend}
     * appellent tous deux cette méthode, qui applique dans les deux cas le même
     * délai anti-spam par rapport au dernier code émis.</p>
     *
     * <p>Ne révèle jamais si l'email correspond à un compte existant : la même
     * réponse est renvoyée dans les deux cas (protection contre l'énumération
     * d'emails).</p>
     *
     * @throws com.campuslink.exception.TooManyRequestsException si le délai anti-spam n'est pas écoulé
     */
    OtpResponse generateAndSend(String email, OtpType type);

    /**
     * Vérifie un code OTP soumis par le client.
     *
     * @throws com.campuslink.exception.InvalidOtpException si le code est incorrect,
     *         expiré, ou qu'aucun code actif n'existe
     * @throws com.campuslink.exception.TooManyRequestsException si le nombre maximal
     *         de tentatives est atteint
     */
    void verify(String email, String code, OtpType type);

}
