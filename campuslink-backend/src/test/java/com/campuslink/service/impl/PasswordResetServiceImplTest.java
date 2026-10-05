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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link PasswordResetServiceImpl}, isolés de Spring et de
 * la base de données grâce à Mockito.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final String EMAIL = "etudiant@campuslink.io";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordResetProperties properties;

    private PasswordResetServiceImpl passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
                userRepository, passwordResetTokenRepository, refreshTokenRepository,
                passwordEncoder, emailService, properties);
    }

    // ===================== requestReset =====================

    @Nested
    class RequestReset {

        @Test
        void shouldDoNothing_whenEmailUnknown() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            passwordResetService.requestReset(EMAIL);

            verify(passwordResetTokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), anyString(), anyInt());
        }

        @Test
        void shouldCreateTokenAndSendEmail_whenUserExists() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                    .thenReturn(Optional.empty());
            when(properties.getTokenExpirationMinutes()).thenReturn(30L);
            when(properties.getResetUrl()).thenReturn("https://app.campuslink.io/reset-password");

            passwordResetService.requestReset(EMAIL);

            verify(passwordResetTokenRepository).deleteByUserAndUsedFalse(user);

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            PasswordResetToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUser()).isEqualTo(user);
            assertThat(savedToken.getTokenHash())
                    .as("Le hash SHA-256 fait 64 caractères hexadécimaux")
                    .hasSize(64)
                    .matches("^[0-9a-f]{64}$");
            assertThat(savedToken.isUsed()).isFalse();

            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendPasswordResetEmail(eq(user), linkCaptor.capture(), eq(30));
            assertThat(linkCaptor.getValue())
                    .startsWith("https://app.campuslink.io/reset-password?token=")
                    .as("Le lien ne doit jamais contenir le hash, uniquement le token brut envoyé au client");
        }

        @Test
        void shouldThrowTooManyRequests_whenCooldownNotElapsed() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            PasswordResetToken recentToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash("abc123")
                    .expiresAt(Instant.now().plusSeconds(1800))
                    .build();
            recentToken.setCreatedAt(Instant.now().minusSeconds(5)); // émis il y a 5s

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                    .thenReturn(Optional.of(recentToken));
            when(properties.getResendCooldownSeconds()).thenReturn(60L);

            assertThatThrownBy(() -> passwordResetService.requestReset(EMAIL))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(passwordResetTokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), anyString(), anyInt());
        }
    }

    // ===================== resetPassword =====================

    @Nested
    class ResetPassword {

        @Test
        void shouldUpdatePassword_markTokenUsed_andRevokeSessions_whenTokenValid() {
            User user = User.builder().email(EMAIL).password("old-hashed-password").build();

            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash("irrelevant-in-test") // recalculé côté service à partir du token brut
                    .expiresAt(Instant.now().plusSeconds(1800))
                    .build();
            token.setId(UUID.randomUUID());

            // Le service recalcule le hash lui-même : on doit stubber avec la même
            // valeur de hash que celle qu'il calculera à partir de "raw-token".
            String rawToken = "raw-token";
            when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewSecret123")).thenReturn("new-hashed-password");
            when(refreshTokenRepository.revokeAllActiveForUser(eq(user), any(Instant.class))).thenReturn(2);

            passwordResetService.resetPassword(rawToken, "NewSecret123");

            assertThat(user.getPassword()).isEqualTo("new-hashed-password");
            assertThat(token.isUsed()).isTrue();
            verify(userRepository).save(user);
            verify(passwordResetTokenRepository).save(token);
            verify(refreshTokenRepository).revokeAllActiveForUser(eq(user), any(Instant.class));
        }

        @Test
        void shouldThrowInvalidToken_whenTokenNotFoundOrAlreadyUsed() {
            when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("unknown-token", "NewSecret123"))
                    .isInstanceOf(InvalidTokenException.class);

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllActiveForUser(any(), any());
        }

        @Test
        void shouldThrowInvalidToken_whenTokenExpired() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash("abc123")
                    .expiresAt(Instant.now().minusSeconds(60)) // déjà expiré
                    .build();

            when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "NewSecret123"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expiré");

            verify(userRepository, never()).save(any());
        }
    }

}
