package com.campuslink.integration;

import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.realtime.integration.UserModerationPort;
import com.campuslink.repository.RefreshTokenRepository;
import com.campuslink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implémentation du {@code UserModerationPort} du module {@code realtime}
 * (DevC), adossée à l'entité {@code User} et au statut de compte déjà
 * existants côté backend principal (mêmes statuts que le module Admin).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserModerationPortImpl implements UserModerationPort {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void suspendre(Long userId, String motif) {
        User user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(user, Instant.now());
        log.info("Utilisateur (legacyId={}) suspendu via le module realtime — motif : {}", userId, motif);
    }

    @Override
    @Transactional
    public void bannir(Long userId, String motif) {
        User user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.BANNED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(user, Instant.now());
        log.info("Utilisateur (legacyId={}) banni via le module realtime — motif : {}", userId, motif);
    }

    @Override
    @Transactional
    public void debannir(Long userId, String motif) {
        User user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        log.info("Utilisateur (legacyId={}) débanni via le module realtime — motif : {}", userId, motif);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findByLegacyId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "legacyId", userId));
    }

}
