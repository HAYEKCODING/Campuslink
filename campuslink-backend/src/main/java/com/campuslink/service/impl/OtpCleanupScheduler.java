package com.campuslink.service.impl;

import com.campuslink.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Tâche planifiée assurant la suppression automatique des codes OTP expirés
 * ou déjà utilisés — évite l'accumulation indéfinie de lignes obsolètes dans
 * {@code otp_codes}.
 *
 * <p>Fréquence pilotée par {@code application.otp.cleanup-cron} (voir
 * {@link com.campuslink.config.OtpProperties}), activée globalement par
 * {@link com.campuslink.config.SchedulingConfig}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final OtpCodeRepository otpCodeRepository;

    @Scheduled(cron = "${application.otp.cleanup-cron}")
    @Transactional
    public void purgeExpiredAndUsedCodes() {
        int deletedCount = otpCodeRepository.deleteExpiredOrUsed(Instant.now());
        if (deletedCount > 0) {
            log.info("Purge OTP : {} code(s) expiré(s) ou utilisé(s) supprimé(s).", deletedCount);
        }
    }

}
