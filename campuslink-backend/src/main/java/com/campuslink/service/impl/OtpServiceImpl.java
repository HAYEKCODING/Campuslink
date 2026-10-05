package com.campuslink.service.impl;

import com.campuslink.config.OtpProperties;
import com.campuslink.dto.response.OtpResponse;
import com.campuslink.entity.OtpCode;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.OtpType;
import com.campuslink.exception.InvalidOtpException;
import com.campuslink.exception.TooManyRequestsException;
import com.campuslink.repository.OtpCodeRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.EmailService;
import com.campuslink.service.OtpService;
import com.campuslink.util.EmailMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Implémentation du module OTP.
 *
 * <p>{@link #generateAndSend} est annotée {@link Propagation#REQUIRES_NEW} : elle
 * peut être appelée depuis une transaction déjà en cours (ex. {@code AuthServiceImpl.register})
 * sans qu'un échec d'envoi d'email ne marque cette transaction englobante comme
 * "rollback-only". Voir le commentaire détaillé sur la méthode.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpProperties otpProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OtpResponse generateAndSend(String rawEmail, OtpType type) {
        String email = rawEmail.trim().toLowerCase();

        Instant expiresAt = Instant.now().plus(otpProperties.getExpirationMinutes(), ChronoUnit.MINUTES);
        Instant resendAvailableAt = Instant.now().plusSeconds(otpProperties.getResendCooldownSeconds());

        Optional<User> maybeUser = userRepository.findByEmail(email);

        if (maybeUser.isEmpty()) {
            // Ne jamais révéler qu'un email n'est pas enregistré : réponse
            // identique au cas de succès, sans rien persister ni envoyer.
            log.debug("Demande de code OTP ({}) pour un email inconnu.", type);
            return OtpResponse.builder()
                    .email(EmailMasker.mask(email))
                    .type(type)
                    .expiresAt(expiresAt)
                    .resendAvailableAt(resendAvailableAt)
                    .build();
        }

        User user = maybeUser.get();
        enforceResendCooldown(user, type);

        // Un seul code actif à la fois par (utilisateur, type) : les codes non
        // utilisés précédemment émis sont invalidés avant d'en créer un nouveau.
        otpCodeRepository.deleteByUserAndTypeAndUsedFalse(user, type);

        String code = generateNumericCode();
        OtpCode otpCode = OtpCode.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiresAt(expiresAt)
                .build();
        otpCodeRepository.save(otpCode);

        emailService.sendOtpCode(user, code, type, (int) otpProperties.getExpirationMinutes());

        log.info("Code OTP ({}) généré et envoyé pour {}", type, email);

        return OtpResponse.builder()
                .email(EmailMasker.mask(email))
                .type(type)
                .expiresAt(expiresAt)
                .resendAvailableAt(resendAvailableAt)
                .build();
    }

    @Override
    @Transactional
    public void verify(String rawEmail, String rawCode, OtpType type) {
        String email = rawEmail.trim().toLowerCase();
        String code = rawCode.trim();

        // Même message générique que le code soit incorrect ou l'email inconnu :
        // ne pas laisser un attaquant distinguer les deux cas.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Code invalide ou expiré."));

        OtpCode otpCode = otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, type)
                .orElseThrow(() -> new InvalidOtpException("Aucun code actif. Veuillez en demander un nouveau."));

        if (otpCode.isExpired()) {
            throw new InvalidOtpException("Ce code a expiré. Veuillez en demander un nouveau.");
        }

        if (otpCode.getAttempts() >= otpProperties.getMaxAttempts()) {
            // Brûle le code pour empêcher toute poursuite de la tentative de force brute.
            otpCode.setUsed(true);
            otpCodeRepository.save(otpCode);
            throw new TooManyRequestsException(
                    "Nombre maximal de tentatives atteint. Veuillez demander un nouveau code.");
        }

        if (!otpCode.getCode().equals(code)) {
            otpCode.setAttempts(otpCode.getAttempts() + 1);
            otpCodeRepository.save(otpCode);
            throw new InvalidOtpException("Code incorrect.");
        }

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        applySideEffects(user, type);

        log.info("Code OTP ({}) vérifié avec succès pour {}", type, email);
    }

    /**
     * Applique les conséquences métier propres à chaque type de code une fois
     * la vérification réussie.
     *
     * <p>Seul {@link OtpType#EMAIL_VERIFICATION} a un effet implémenté ici
     * (passage du compte à {@code ACTIVE}). Les effets pour {@code PASSWORD_RESET},
     * {@code PHONE_VERIFICATION} et {@code TWO_FACTOR_AUTH} relèvent des modules
     * qui en ont besoin (réinitialisation effective du mot de passe, mise à jour
     * du numéro sur le profil, émission d'un token de session) et sortent du
     * périmètre du module OTP lui-même — non implémentés ici.</p>
     */
    private void applySideEffects(User user, OtpType type) {
        if (type == OtpType.EMAIL_VERIFICATION) {
            user.setEmailVerified(true);
            if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
                user.setStatus(AccountStatus.ACTIVE);
            }
            userRepository.save(user);
        }
    }

    private void enforceResendCooldown(User user, OtpType type) {
        otpCodeRepository.findFirstByUserAndTypeOrderByCreatedAtDesc(user, type)
                .ifPresent(lastCode -> {
                    Instant nextAllowedAt = lastCode.getCreatedAt()
                            .plusSeconds(otpProperties.getResendCooldownSeconds());

                    if (Instant.now().isBefore(nextAllowedAt)) {
                        long remainingSeconds = Duration.between(Instant.now(), nextAllowedAt).getSeconds();
                        throw new TooManyRequestsException(
                                "Veuillez patienter " + remainingSeconds
                                        + " seconde(s) avant de redemander un code.");
                    }
                });
    }

    private String generateNumericCode() {
        int length = otpProperties.getLength();
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }

}
