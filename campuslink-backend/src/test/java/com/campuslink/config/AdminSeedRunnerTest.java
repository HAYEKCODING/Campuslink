package com.campuslink.config;

import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du seed administrateur ({@link AdminSeedRunner}) — critère de
 * déploiement sur base vierge : sans compte ADMIN, aucun accès possible
 * à {@code /admin/**}.
 *
 * <p>Les champs de configuration ({@code @Value}) sont injectés via
 * {@link ReflectionTestUtils} pour tester les quatre branches du runner
 * sans redémarrer de contexte Spring.</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminSeedRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeedRunner runner;

    @BeforeEach
    void setUp() {
        // Par défaut : aucune variable d'environnement renseignée.
        ReflectionTestUtils.setField(runner, "adminEmail", "");
        ReflectionTestUtils.setField(runner, "adminPassword", "");
    }

    private void configureEnv(String email, String password) {
        ReflectionTestUtils.setField(runner, "adminEmail", email);
        ReflectionTestUtils.setField(runner, "adminPassword", password);
    }

    @Test
    @DisplayName("Variables absentes : no-op (aucune lecture repository, démarrage jamais bloqué)")
    void shouldDoNothing_whenEnvVarsMissing() {
        runner.run(null);

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Variables vides (espaces) : no-op — un mot de passe vide ne crée jamais de compte")
    void shouldDoNothing_whenEnvVarsBlank() {
        configureEnv("   ", "   ");

        runner.run(null);

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Compte déjà présent : no-op (idempotence des redéploiements)")
    void shouldDoNothing_whenAccountAlreadyExists() {
        configureEnv("Admin@Campuslink.io", "Str0ng-P@ss!");
        when(userRepository.findByEmail("admin@campuslink.io"))
                .thenReturn(Optional.of(User.builder().email("admin@campuslink.io").build()));

        runner.run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("Rôle ADMIN absent de la base : erreur explicite (schema.sql non exécuté)")
    void shouldThrowIllegalState_whenAdminRoleMissing() {
        configureEnv("admin@campuslink.io", "Str0ng-P@ss!");
        when(userRepository.findByEmail("admin@campuslink.io")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN")
                .hasMessageContaining("schema.sql");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Compte absent : création avec BCrypt, statut ACTIVE et rôle ADMIN")
    void shouldCreateAdmin_whenAccountMissing() {
        configureEnv("  Admin@Campuslink.IO ", "Str0ng-P@ss!");
        when(userRepository.findByEmail("admin@campuslink.io")).thenReturn(Optional.empty());

        Role adminRole = Role.builder().name(RoleName.ADMIN).build();
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("Str0ng-P@ss!")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        // Email normalisé : trim + minuscules (case-insensitive en base).
        assertThat(saved.getEmail()).isEqualTo("admin@campuslink.io");
        assertThat(saved.getPassword()).isEqualTo("$2a$hashed");
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getRoles()).contains(adminRole);

        verify(passwordEncoder).encode("Str0ng-P@ss!");
    }

}
