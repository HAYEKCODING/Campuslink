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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

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
 * Tests unitaires de {@link OtpServiceImpl}, isolés de Spring et de la base
 * de données grâce à Mockito.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final String EMAIL = "etudiant@campuslink.io";

    @Mock
    private OtpCodeRepository otpCodeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpProperties otpProperties;

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(otpCodeRepository, userRepository, emailService, otpProperties);
    }

    // ===================== generateAndSend =====================

    @Nested
    class GenerateAndSend {

        @Test
        void shouldReturnGenericResponse_whenEmailUnknown() {
            when(otpProperties.getExpirationMinutes()).thenReturn(10L);
            when(otpProperties.getResendCooldownSeconds()).thenReturn(60L);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            OtpResponse response = otpService.generateAndSend(EMAIL, OtpType.EMAIL_VERIFICATION);

            // Réponse renvoyée normalement...
            assertThat(response.getType()).isEqualTo(OtpType.EMAIL_VERIFICATION);
            assertThat(response.getEmail()).contains("*");

            // ...mais rien n'est réellement créé ni envoyé (anti-énumération).
            verify(otpCodeRepository, never()).save(any());
            verify(emailService, never()).sendOtpCode(any(), anyString(), any(), anyInt());
        }

        @Test
        void shouldCreateCodeAndSendEmail_whenUserExists() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            when(otpProperties.getExpirationMinutes()).thenReturn(10L);
            when(otpProperties.getResendCooldownSeconds()).thenReturn(60L);
            when(otpProperties.getLength()).thenReturn(6);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.empty());

            OtpResponse response = otpService.generateAndSend(EMAIL, OtpType.EMAIL_VERIFICATION);

            assertThat(response.getType()).isEqualTo(OtpType.EMAIL_VERIFICATION);

            verify(otpCodeRepository).deleteByUserAndTypeAndUsedFalse(user, OtpType.EMAIL_VERIFICATION);
            verify(otpCodeRepository).save(any(OtpCode.class));
            verify(emailService).sendOtpCode(eq(user), anyString(), eq(OtpType.EMAIL_VERIFICATION), eq(10));
        }

        @Test
        void shouldThrowTooManyRequests_whenCooldownNotElapsed() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            OtpCode recentCode = OtpCode.builder()
                    .user(user)
                    .code("123456")
                    .type(OtpType.EMAIL_VERIFICATION)
                    .expiresAt(Instant.now().plusSeconds(600))
                    .build();
            recentCode.setCreatedAt(Instant.now().minusSeconds(10)); // émis il y a 10s

            when(otpProperties.getExpirationMinutes()).thenReturn(10L);
            when(otpProperties.getResendCooldownSeconds()).thenReturn(60L); // cooldown 60s
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(recentCode));

            assertThatThrownBy(() -> otpService.generateAndSend(EMAIL, OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(otpCodeRepository, never()).save(any());
            verify(emailService, never()).sendOtpCode(any(), anyString(), any(), anyInt());
        }
    }

    // ===================== verify =====================

    @Nested
    class Verify {

        @Test
        void shouldSucceed_andActivateAccount_whenCodeCorrectAndTypeEmailVerification() {
            User user = User.builder()
                    .email(EMAIL)
                    .password("hashed")
                    .status(AccountStatus.PENDING_VERIFICATION)
                    .emailVerified(false)
                    .build();

            OtpCode otpCode = OtpCode.builder()
                    .user(user)
                    .code("123456")
                    .type(OtpType.EMAIL_VERIFICATION)
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();

            when(otpProperties.getMaxAttempts()).thenReturn(5);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(otpCode));

            otpService.verify(EMAIL, "123456", OtpType.EMAIL_VERIFICATION);

            assertThat(otpCode.isUsed()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(userRepository).save(user);
        }

        @Test
        void shouldThrowInvalidOtp_whenNoActiveCodeExists() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456", OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(InvalidOtpException.class);
        }

        @Test
        void shouldThrowInvalidOtp_whenUserUnknown() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456", OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(InvalidOtpException.class);

            verify(otpCodeRepository, never()).findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(any(), any());
        }

        @Test
        void shouldThrowInvalidOtp_whenCodeExpired() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            OtpCode expiredCode = OtpCode.builder()
                    .user(user)
                    .code("123456")
                    .type(OtpType.EMAIL_VERIFICATION)
                    .expiresAt(Instant.now().minusSeconds(60)) // déjà expiré
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(expiredCode));

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456", OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(InvalidOtpException.class)
                    .hasMessageContaining("expiré");
        }

        @Test
        void shouldIncrementAttempts_whenCodeIncorrect() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            OtpCode otpCode = OtpCode.builder()
                    .user(user)
                    .code("123456")
                    .type(OtpType.EMAIL_VERIFICATION)
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();

            when(otpProperties.getMaxAttempts()).thenReturn(5);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(otpCode));

            assertThatThrownBy(() -> otpService.verify(EMAIL, "999999", OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(InvalidOtpException.class);

            assertThat(otpCode.getAttempts()).isEqualTo(1);
            assertThat(otpCode.isUsed()).isFalse();
            verify(otpCodeRepository).save(otpCode);
        }

        @Test
        void shouldThrowTooManyRequests_andBurnCode_whenMaxAttemptsReached() {
            User user = User.builder().email(EMAIL).password("hashed").build();

            OtpCode otpCode = OtpCode.builder()
                    .user(user)
                    .code("123456")
                    .type(OtpType.EMAIL_VERIFICATION)
                    .expiresAt(Instant.now().plusSeconds(300))
                    .attempts(5)
                    .build();

            when(otpProperties.getMaxAttempts()).thenReturn(5);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(otpCodeRepository.findFirstByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(otpCode));

            assertThatThrownBy(() -> otpService.verify(EMAIL, "123456", OtpType.EMAIL_VERIFICATION))
                    .isInstanceOf(TooManyRequestsException.class);

            assertThat(otpCode.isUsed())
                    .as("Le code doit être brûlé après le nombre maximal de tentatives")
                    .isTrue();
        }
    }

}
