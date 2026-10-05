package com.campuslink.service.impl;

import com.campuslink.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Tâche planifiée assurant la suppression automatique des tokens de
 * réinitialisation de mot de passe expirés ou déjà utilisés.
 *
 * <p>Fréquence pilotée par {@code application.password-reset.cleanup-cron}
 * (voir {@link com.campuslink.config.PasswordResetProperties}).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "${application.password-reset.cleanup-cron}")
    @Transactional
    public void purgeExpiredAndUsedTokens() {
        int deletedCount = passwordResetTokenRepository.deleteExpiredOrUsed(Instant.now());
        if (deletedCount > 0) {
            log.info("Purge tokens de réinitialisation : {} supprimé(s).", deletedCount);
        }
    }

}
