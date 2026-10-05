package com.campuslink.controller;

import com.campuslink.dto.request.ForgotPasswordRequest;
import com.campuslink.dto.request.LoginRequest;
import com.campuslink.dto.request.RefreshTokenRequest;
import com.campuslink.dto.request.RegisterRequest;
import com.campuslink.dto.request.ResetPasswordRequest;
import com.campuslink.dto.response.AuthResponse;
import com.campuslink.dto.response.UserResponse;
import com.campuslink.enums.AccountStatus;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.InvalidTokenException;
import com.campuslink.exception.TooManyRequestsException;
import com.campuslink.security.JwtService;
import com.campuslink.service.AuthService;
import com.campuslink.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du module d'authentification, isolés du reste de
 * l'application via {@link WebMvcTest} : seuls {@link AuthController} et
 * {@code GlobalExceptionHandler} sont chargés, {@link AuthService} est mocké.
 *
 * <p>La chaîne de sécurité complète (JWT, filtres) n'est volontairement pas
 * chargée ici — ces tests valident le contrat HTTP du controller (statuts,
 * forme des réponses JSON, remontée des erreurs de validation), pas la
 * sécurité elle-même.</p>
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    /**
     * Fourni uniquement pour satisfaire la dépendance de {@code JwtAuthenticationFilter},
     * que la tranche {@code @WebMvcTest} inclut automatiquement (les beans {@code Filter}
     * font partie de la slice) mais dont les filtres ne s'exécutent pas ici
     * ({@code addFilters = false}).
     */
    @MockBean
    private JwtService jwtService;

    // ===================== POST /auth/register =====================

    @Test
    void register_shouldReturn201_whenPayloadValid() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("etudiant@campuslink.io")
                .password("Secret123")
                .confirmPassword("Secret123")
                .firstName("Awa")
                .lastName("Kone")
                .build();

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .email("etudiant@campuslink.io")
                .status(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .roles(Set.of("STUDENT"))
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("etudiant@campuslink.io"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void register_shouldReturn400_whenPasswordsDoNotMatch() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("etudiant@campuslink.io")
                .password("Secret123")
                .confirmPassword("Different123")
                .firstName("Awa")
                .lastName("Kone")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'confirmPassword')]").exists());
    }

    @Test
    void register_shouldReturn400_whenEmailInvalid() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("pas-un-email")
                .password("Secret123")
                .confirmPassword("Secret123")
                .firstName("Awa")
                .lastName("Kone")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyRegistered() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("existant@campuslink.io")
                .password("Secret123")
                .confirmPassword("Secret123")
                .firstName("Awa")
                .lastName("Kone")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Un compte existe déjà avec l'email : existant@campuslink.io"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ===================== POST /auth/login =====================

    @Test
    void login_shouldReturn200_withTokens_whenCredentialsValid() throws Exception {
        LoginRequest request = new LoginRequest("etudiant@campuslink.io", "Secret123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(UserResponse.builder().email("etudiant@campuslink.io").build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_shouldReturn400_whenEmailMissing() throws Exception {
        LoginRequest request = new LoginRequest("", "Secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===================== POST /auth/refresh-token =====================

    @Test
    void refreshToken_shouldReturn200_whenTokenValid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(UserResponse.builder().email("etudiant@campuslink.io").build())
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void refreshToken_shouldReturn400_whenRefreshTokenBlank() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===================== POST /auth/logout =====================

    @Test
    void logout_shouldReturn200_andDelegateToService() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(any(RefreshTokenRequest.class));
    }

    // ===================== POST /auth/forgot-password =====================

    @Test
    void forgotPassword_shouldReturn200_andAlwaysSameMessage() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("etudiant@campuslink.io");

        doNothing().when(passwordResetService).requestReset("etudiant@campuslink.io");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Si un compte existe pour cet email, un lien de réinitialisation a été envoyé."));

        verify(passwordResetService).requestReset("etudiant@campuslink.io");
    }

    @Test
    void forgotPassword_shouldReturn400_whenEmailInvalid() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("pas-un-email");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(passwordResetService, never()).requestReset(any());
    }

    @Test
    void forgotPassword_shouldReturn429_whenCooldownActive() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("etudiant@campuslink.io");

        doThrow(new TooManyRequestsException("Veuillez patienter 30 seconde(s)."))
                .when(passwordResetService).requestReset("etudiant@campuslink.io");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    // ===================== POST /auth/reset-password =====================

    @Test
    void resetPassword_shouldReturn200_whenTokenValid() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("a-valid-token")
                .newPassword("NewSecret123")
                .confirmPassword("NewSecret123")
                .build();

        doNothing().when(passwordResetService).resetPassword(eq("a-valid-token"), eq("NewSecret123"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(passwordResetService).resetPassword("a-valid-token", "NewSecret123");
    }

    @Test
    void resetPassword_shouldReturn400_whenPasswordsDoNotMatch() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("a-valid-token")
                .newPassword("NewSecret123")
                .confirmPassword("Different123")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'confirmPassword')]").exists());

        verify(passwordResetService, never()).resetPassword(any(), any());
    }

    @Test
    void resetPassword_shouldReturn400_whenTokenInvalidOrExpired() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("NewSecret123")
                .confirmPassword("NewSecret123")
                .build();

        doThrow(new InvalidTokenException("Ce lien a expiré. Veuillez en demander un nouveau."))
                .when(passwordResetService).resetPassword(eq("expired-token"), any());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ce lien a expiré. Veuillez en demander un nouveau."));
    }

    @Test
    void resetPassword_shouldReturn400_whenPasswordTooWeak() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("a-valid-token")
                .newPassword("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(passwordResetService, never()).resetPassword(any(), any());
    }

}
