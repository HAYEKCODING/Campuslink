package com.campuslink.controller;

import com.campuslink.dto.request.ForgotPasswordRequest;
import com.campuslink.dto.request.LoginRequest;
import com.campuslink.dto.request.RefreshTokenRequest;
import com.campuslink.dto.request.RegisterRequest;
import com.campuslink.dto.request.ResetPasswordRequest;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.AuthResponse;
import com.campuslink.dto.response.UserResponse;
import com.campuslink.service.AuthService;
import com.campuslink.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints d'authentification : inscription, connexion, rafraîchissement de
 * token, déconnexion, ainsi que la réinitialisation de mot de passe (Forgot
 * Password) — regroupée ici plutôt que dans un controller séparé car elle
 * relève du même domaine "accès au compte" et partage déjà le préfixe
 * {@code /auth} public.
 *
 * <p>Le controller ne contient aucune logique métier — il valide la requête
 * (via {@code @Valid}) et délègue intégralement aux services, conformément
 * au principe SRP.</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, rafraîchissement, déconnexion et réinitialisation de mot de passe")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un nouveau compte utilisateur")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ApiResponse.success(response, "Compte créé avec succès. Vous pouvez maintenant vous connecter.");
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur et émettre une paire de tokens")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response, "Connexion réussie.");
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Échanger un refresh token valide contre une nouvelle paire de tokens")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ApiResponse.success(response, "Token rafraîchi avec succès.");
    }

    @PostMapping("/logout")
    @Operation(summary = "Révoquer un refresh token (déconnexion)")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(null, "Déconnexion réussie.");
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Demander un lien de réinitialisation de mot de passe",
            description = "Envoie un email contenant un lien de réinitialisation si l'adresse correspond à un "
                    + "compte existant. Renvoie systématiquement le même message de succès, que le compte "
                    + "existe ou non, afin de ne pas exposer d'oracle d'énumération d'emails."
    )
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ApiResponse.success(null, "Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.");
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Réinitialiser le mot de passe à partir d'un token valide",
            description = "Consomme un token de réinitialisation à usage unique et définit un nouveau mot de "
                    + "passe. Révoque au passage toutes les sessions actives de l'utilisateur."
    )
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success(null, "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.");
    }

}
