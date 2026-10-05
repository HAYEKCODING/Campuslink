package com.campuslink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active le support des tâches planifiées ({@code @Scheduled}), utilisé
 * notamment par {@link com.campuslink.service.impl.OtpCleanupScheduler}
 * pour la suppression automatique des codes OTP expirés/utilisés.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
