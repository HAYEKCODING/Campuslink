package com.campuslink.service;

import com.campuslink.dto.request.LoginRequest;
import com.campuslink.dto.request.RefreshTokenRequest;
import com.campuslink.dto.request.RegisterRequest;
import com.campuslink.dto.response.AuthResponse;
import com.campuslink.dto.response.UserResponse;

/**
 * Contrat métier du module d'authentification.
 *
 * <p>Le controller ne dépend que de cette interface (principe DIP) — l'implémentation
 * concrète ({@code AuthServiceImpl}) reste substituable, notamment pour les tests.</p>
 */
public interface AuthService {

    /**
     * Crée un nouveau compte utilisateur (statut initial : {@code PENDING_VERIFICATION}).
     *
     * <p>Ne renvoie volontairement pas de tokens : l'inscription et la connexion
     * restent deux actions distinctes côté client.</p>
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authentifie un utilisateur par email/mot de passe et émet une nouvelle
     * paire de tokens (access + refresh).
     */
    AuthResponse login(LoginRequest request);

    /**
     * Échange un refresh token valide contre une nouvelle paire de tokens.
     * Applique une rotation : l'ancien refresh token est révoqué dans le même appel.
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Révoque le refresh token fourni. Opération idempotente : un token déjà
     * révoqué, invalide ou inconnu ne provoque pas d'erreur.
     */
    void logout(RefreshTokenRequest request);

}
