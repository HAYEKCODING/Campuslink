package com.campuslink.service.impl;

import com.campuslink.config.PasswordResetProperties;
import com.campuslink.entity.PasswordResetToken;
import com.campuslink.entity.User;
import com.campuslink.exception.InvalidTokenException;
import com.campuslink.exception.TooManyRequestsException;
import com.campuslink.repository.PasswordResetTokenRepository;
import com.campuslink.repository.RefreshTokenRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.EmailService;
import com.campuslink.service.PasswordResetService;
import com.campuslink.util.EmailMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Implémentation du module Forgot Password.
 *
 * <p>{@link #requestReset} est annotée {@link Propagation#REQUIRES_NEW} pour
 * la même raison que {@code OtpServiceImpl.generateAndSend} : un échec
 * d'envoi d'email ne doit jamais pouvoir marquer une transaction englobante
 * comme rollback-only si cette méthode est un jour appelée depuis un autre
 * flux transactionnel.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetProperties properties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();

        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            // Ne jamais révéler qu'un email n'est pas enregistré : aucune
            // action, mais pas d'exception non plus — le contrôleur renvoie
            // le même message de succès dans les deux cas.
            log.debug("Demande de réinitialisation pour un email inconnu.");
            return;
        }

        User user = maybeUser.get();
        enforceCooldown(user);

        // Un seul lien actif à la fois par utilisateur.
        passwordResetTokenRepository.deleteByUserAndUsedFalse(user);

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(properties.getTokenExpirationMinutes(), ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.save(token);

        String resetLink = buildResetLink(rawToken);
        emailService.sendPasswordResetEmail(user, resetLink, (int) properties.getTokenExpirationMinutes());

        log.info("Lien de réinitialisation généré pour {}", EmailMasker.mask(email));
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Lien invalide ou déjà utilisé."));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Ce lien a expiré. Veuillez en demander un nouveau.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Un changement de mot de passe doit invalider toute session déjà
        // ouverte ailleurs (protection en cas de compte compromis).
        int revokedCount = refreshTokenRepository.revokeAllActiveForUser(user, Instant.now());

        log.info("Mot de passe réinitialisé pour {} — {} session(s) révoquée(s).",
                EmailMasker.mask(user.getEmail()), revokedCount);
    }

    private void enforceCooldown(User user) {
        passwordResetTokenRepository.findFirstByUserOrderByCreatedAtDesc(user).ifPresent(lastToken -> {
            Instant nextAllowedAt = lastToken.getCreatedAt().plusSeconds(properties.getResendCooldownSeconds());

            if (Instant.now().isBefore(nextAllowedAt)) {
                long remainingSeconds = Duration.between(Instant.now(), nextAllowedAt).getSeconds();
                throw new TooManyRequestsException(
                        "Veuillez patienter " + remainingSeconds
                                + " seconde(s) avant de redemander un lien de réinitialisation.");
            }
        });
    }

    private String buildResetLink(String rawToken) {
        String separator = properties.getResetUrl().contains("?") ? "&" : "?";
        return properties.getResetUrl() + separator + "token=" + rawToken;
    }

    /**
     * Génère un token aléatoire cryptographiquement sûr (256 bits), encodé en
     * base64url sans padding pour rester directement utilisable dans une URL.
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Empreinte SHA-256 (hexadécimale) du token brut — c'est cette valeur, et
     * uniquement elle, qui est persistée en base (voir {@link PasswordResetToken}).
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 fait partie du socle standard de toute JVM conforme :
            // ne peut normalement jamais se produire.
            throw new IllegalStateException("Algorithme SHA-256 indisponible.", ex);
        }
    }

}
