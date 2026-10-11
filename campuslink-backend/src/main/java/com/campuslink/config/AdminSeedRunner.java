package com.campuslink.config;

import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.util.EmailMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Crée le compte administrateur initial au démarrage, à partir de variables
 * d'environnement — requis après une base vierge (le seed des rôles seul ne
 * fournit aucun compte capable d'accéder à {@code /admin/**}).
 *
 * <p>Configuration :</p>
 * <ul>
 *     <li>{@code ADMIN_EMAIL} ({@code campuslink.admin.email})</li>
 *     <li>{@code ADMIN_PASSWORD} ({@code campuslink.admin.password})</li>
 * </ul>
 *
 * <p>Comportement :</p>
 * <ul>
 *     <li>Variables absentes ou vides (dev local, tests) : no-op — le runner
 *         ne doit jamais bloquer un démarrage ni créer de compte par erreur.</li>
 *     <li>Compte déjà présent (email existant) : no-op, le démarrage est
 *         idempotent (redéploiements successifs).</li>
 *     <li>Rôle {@code ADMIN} absent de la table {@code roles} : erreur explicite
 *         (la base n'a pas été initialisée avec {@code database/schema.sql}).</li>
 * </ul>
 *
 * <p>Le mot de passe est hashé via BCrypt comme tout autre compte ; il n'est
 * jamais journalisé. Recommandation de déploiement : définir un mot de passe
 * fort une première fois, puis le révoquer/remplacer après la création.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${campuslink.admin.email:}")
    private String adminEmail;

    @Value("${campuslink.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.debug("Seed admin ignore : campuslink.admin.email / campuslink.admin.password non renseignes.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.info("Seed admin ignore : le compte {} existe deja.", EmailMasker.mask(normalizedEmail));
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle ADMIN introuvable : exécuter database/schema.sql (seed des rôles) avant le démarrage."));

        User admin = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(adminPassword))
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        admin.addRole(adminRole);

        userRepository.save(admin);
        log.info("Compte administrateur cree : {} (role ADMIN).", EmailMasker.mask(normalizedEmail));
    }

}
