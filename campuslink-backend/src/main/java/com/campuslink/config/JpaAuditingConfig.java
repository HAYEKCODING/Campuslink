package com.campuslink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Active l'audit JPA (renseignement automatique des champs
 * {@code createdAt} / {@code updatedAt} définis dans {@link com.campuslink.entity.BaseEntity}).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
