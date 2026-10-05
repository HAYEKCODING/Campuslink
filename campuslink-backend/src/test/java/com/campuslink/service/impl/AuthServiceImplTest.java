package com.campuslink.service.impl;

import com.campuslink.dto.request.LoginRequest;
import com.campuslink.dto.request.RefreshTokenRequest;
import com.campuslink.dto.request.RegisterRequest;
import com.campuslink.dto.response.AuthResponse;
import com.campuslink.dto.response.UserResponse;
import com.campuslink.entity.RefreshToken;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.RoleName;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.UnauthorizedException;
import com.campuslink.mapper.UserMapper;
import com.campuslink.repository.RefreshTokenRepository;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link AuthServiceImpl}, isolés de Spring et de la base
 * de données grâce à Mockito (toutes les dépendances sont mockées).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OtpService otpService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, authenticationManager, jwtService, userMapper, otpService);
    }

    // ===================== register =====================

    @Nested
    class Register {

        @Test
        void shouldCreateUser_whenEmailNotTaken() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("Etudiant@Campuslink.io")
                    .password("Secret123")
                    .confirmPassword("Secret123")
                    .firstName("Awa")
                    .lastName("Kone")
                    .build();

            Role studentRole = Role.builder().name(RoleName.STUDENT).build();

            when(userRepository.existsByEmail("etudiant@campuslink.io")).thenReturn(false);
            when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
            when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toUserResponse(any(User.class)))
                    .thenReturn(UserResponse.builder().email("etudiant@campuslink.io").build());

            UserResponse response = authService.register(request);

            assertThat(response.getEmail()).isEqualTo("etudiant@campuslink.io");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            // L'email est normalisé (minuscules, sans espaces).
            assertThat(savedUser.getEmail()).isEqualTo("etudiant@campuslink.io");
            assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
            assertThat(savedUser.getRoles()).contains(studentRole);
            assertThat(savedUser.getProfile()).isNotNull();
            assertThat(savedUser.getProfile().getFirstName()).isEqualTo("Awa");
            assertThat(savedUser.getProfile().getUser()).isEqualTo(savedUser);

            verify(otpService).generateAndSend(eq("etudiant@campuslink.io"), eq(com.campuslink.enums.OtpType.EMAIL_VERIFICATION));
        }

        @Test
        void shouldNotFail_whenOtpEmailSendingThrows() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("nouveau@campuslink.io")
                    .password("Secret123")
                    .confirmPassword("Secret123")
                    .firstName("Awa")
                    .lastName("Kone")
                    .build();

            Role studentRole = Role.builder().name(RoleName.STUDENT).build();

            when(userRepository.existsByEmail("nouveau@campuslink.io")).thenReturn(false);
            when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
            when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toUserResponse(any(User.class)))
                    .thenReturn(UserResponse.builder().email("nouveau@campuslink.io").build());
            when(otpService.generateAndSend(any(), any()))
                    .thenThrow(new RuntimeException("SMTP indisponible"));

            // L'inscription doit réussir malgré l'échec d'envoi de l'email de vérification.
            UserResponse response = authService.register(request);

            assertThat(response.getEmail()).isEqualTo("nouveau@campuslink.io");
        }

        @Test
        void shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("existant@campuslink.io")
                    .password("Secret123")
                    .confirmPassword("Secret123")
                    .firstName("Awa")
                    .lastName("Kone")
                    .build();

            when(userRepository.existsByEmail("existant@campuslink.io")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalStateException_whenStudentRoleMissingFromDatabase() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("nouveau@campuslink.io")
                    .password("Secret123")
                    .confirmPassword("Secret123")
                    .firstName("Awa")
                    .lastName("Kone")
                    .build();

            when(userRepository.existsByEmail("nouveau@campuslink.io")).thenReturn(false);
            when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("STUDENT");
        }
    }

    // ===================== login =====================

    @Nested
    class Login {

        @Test
        void shouldReturnAuthResponse_whenCredentialsValid() {
            LoginRequest request = new LoginRequest("etudiant@campuslink.io", "Secret123");

            User user = User.builder()
                    .email("etudiant@campuslink.io")
                    .password("hashed-password")
                    .roles(Set.of())
                    .build();
            UserPrincipal principal = new UserPrincipal(user);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.generateAccessToken(principal)).thenReturn("access-token");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);

            // Simule l'attribution de l'id par la base au moment du save().
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken rt = invocation.getArgument(0);
                rt.setId(UUID.randomUUID());
                return rt;
            });
            when(jwtService.generateRefreshToken(eq(principal), any(UUID.class))).thenReturn("refresh-token");
            when(userMapper.toUserResponse(user)).thenReturn(UserResponse.builder().email(user.getEmail()).build());

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(900L);
            assertThat(user.getLastLoginAt()).isNotNull();
        }

        @Test
        void shouldPropagateException_whenCredentialsInvalid() {
            LoginRequest request = new LoginRequest("etudiant@campuslink.io", "wrong-password");

            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateAccessToken(any());
        }
    }

    // ===================== refreshToken =====================

    @Nested
    class RefreshTokenFlow {

        @Test
        void shouldRotateToken_whenRefreshTokenValid() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            UUID tokenId = UUID.randomUUID();

            User user = User.builder()
                    .email("etudiant@campuslink.io")
                    .password("hashed-password")
                    .roles(Set.of())
                    .build();

            RefreshToken storedToken = RefreshToken.builder()
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            storedToken.setId(tokenId);

            when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
            when(jwtService.extractTokenId("valid-refresh-token")).thenReturn(tokenId);
            when(jwtService.extractUsername("valid-refresh-token")).thenReturn("etudiant@campuslink.io");
            when(refreshTokenRepository.findByIdAndRevokedFalse(tokenId)).thenReturn(Optional.of(storedToken));
            when(userRepository.findByEmail("etudiant@campuslink.io")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> {
                        RefreshToken persisted = invocation.getArgument(0);
                        // La base assigne l'identifiant (jti) : sans cela, generateRefreshToken
                        // serait appelé avec un id null et ne correspondrait plus au stub any(UUID.class).
                        if (persisted.getId() == null) {
                            persisted.setId(UUID.randomUUID());
                        }
                        return persisted;
                    });
            when(jwtService.generateRefreshToken(any(), any(UUID.class))).thenReturn("new-refresh-token");
            when(userMapper.toUserResponse(user)).thenReturn(UserResponse.builder().email(user.getEmail()).build());

            AuthResponse response = authService.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(storedToken.isRevoked())
                    .as("L'ancien refresh token doit être révoqué (rotation)")
                    .isTrue();
        }

        @Test
        void shouldThrowUnauthorized_whenTokenIsNotRefreshType() {
            RefreshTokenRequest request = new RefreshTokenRequest("an-access-token");

            when(jwtService.isRefreshToken("an-access-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(refreshTokenRepository, never()).findByIdAndRevokedFalse(any());
        }

        @Test
        void shouldThrowUnauthorized_whenTokenRevokedOrUnknown() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
            UUID tokenId = UUID.randomUUID();

            when(jwtService.isRefreshToken("revoked-token")).thenReturn(true);
            when(jwtService.extractTokenId("revoked-token")).thenReturn(tokenId);
            when(jwtService.extractUsername("revoked-token")).thenReturn("etudiant@campuslink.io");
            when(refreshTokenRepository.findByIdAndRevokedFalse(tokenId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ===================== logout =====================

    @Nested
    class Logout {

        @Test
        void shouldRevokeToken_whenTokenValid() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            UUID tokenId = UUID.randomUUID();

            RefreshToken storedToken = RefreshToken.builder()
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            storedToken.setId(tokenId);

            when(jwtService.extractTokenId("valid-refresh-token")).thenReturn(tokenId);
            when(refreshTokenRepository.findByIdAndRevokedFalse(tokenId)).thenReturn(Optional.of(storedToken));

            authService.logout(request);

            assertThat(storedToken.isRevoked()).isTrue();
            verify(refreshTokenRepository, times(1)).save(storedToken);
        }

        @Test
        void shouldNotThrow_whenTokenAlreadyInvalid() {
            RefreshTokenRequest request = new RefreshTokenRequest("malformed-token");

            when(jwtService.extractTokenId("malformed-token"))
                    .thenThrow(new IllegalArgumentException("Token malformé"));

            authService.logout(request); // ne doit lever aucune exception

            verify(refreshTokenRepository, never()).save(any());
        }
    }

}
