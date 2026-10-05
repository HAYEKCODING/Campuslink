package com.campuslink.integration;

import com.campuslink.entity.Profile;
import com.campuslink.entity.User;
import com.campuslink.realtime.integration.UserDirectoryPort;
import com.campuslink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation du {@code UserDirectoryPort} du module {@code realtime},
 * adossée aux entités du Backend Core ({@code User} / {@code Profile}).
 *
 * <p>Aucune exception n'est levée : un utilisateur inconnu, ou un compte sans
 * profil, produit simplement un aperçu partiellement vide — la liste des matchs
 * reste affichable.</p>
 */
@Component
@RequiredArgsConstructor
public class UserDirectoryPortImpl implements UserDirectoryPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ApercuUtilisateur> trouverApercu(Long legacyId) {
        if (legacyId == null) {
            return Optional.empty();
        }
        return userRepository.findByLegacyId(legacyId)
                .map(user -> {
                    Profile profile = user.getProfile();
                    String nom = nomComplet(profile);
                    String photoUrl = profile != null ? profile.getAvatarUrl() : null;
                    UUID profilId = profile != null ? profile.getId() : null;
                    return new ApercuUtilisateur(legacyId, profilId, nom, photoUrl);
                });
    }

    private String nomComplet(Profile profile) {
        if (profile == null) {
            return null;
        }
        String nom = String.join(" ",
                trimToNull(profile.getFirstName()),
                trimToNull(profile.getLastName()));
        return nom.isBlank() ? null : nom.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
