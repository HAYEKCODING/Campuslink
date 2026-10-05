package com.campuslink.controller;

import com.campuslink.dto.request.OtpRequest;
import com.campuslink.dto.request.VerifyOtpRequest;
import com.campuslink.dto.response.OtpResponse;
import com.campuslink.enums.OtpType;
import com.campuslink.exception.InvalidOtpException;
import com.campuslink.exception.TooManyRequestsException;
import com.campuslink.security.JwtService;
import com.campuslink.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du module OTP, isolés via {@link WebMvcTest}
 * (voir {@link com.campuslink.controller.AuthControllerTest} pour le
 * raisonnement détaillé sur {@code addFilters = false}).
 */
@WebMvcTest(controllers = OtpController.class)
@AutoConfigureMockMvc(addFilters = false)
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    /** Voir {@link AuthControllerTest} : nécessaire pour la slice {@code @WebMvcTest}. */
    @MockBean
    private JwtService jwtService;

    // ===================== POST /otp/send & /otp/resend =====================

    @Test
    void send_shouldReturn200_withMaskedEmail() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .email("etudiant@campuslink.io")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        OtpResponse response = OtpResponse.builder()
                .email("e****t@campuslink.io")
                .type(OtpType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(600))
                .resendAvailableAt(Instant.now().plusSeconds(60))
                .build();

        when(otpService.generateAndSend(eq("etudiant@campuslink.io"), eq(OtpType.EMAIL_VERIFICATION)))
                .thenReturn(response);

        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("e****t@campuslink.io"))
                .andExpect(jsonPath("$.data.type").value("EMAIL_VERIFICATION"));
    }

    @Test
    void send_shouldReturn400_whenEmailInvalid() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .email("pas-un-email")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void send_shouldReturn429_whenCooldownActive() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .email("etudiant@campuslink.io")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        when(otpService.generateAndSend(any(), any()))
                .thenThrow(new TooManyRequestsException("Veuillez patienter 42 seconde(s) avant de redemander un code."));

        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resend_shouldReturn200_andDelegateToSameServiceMethod() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .email("etudiant@campuslink.io")
                .type(OtpType.PASSWORD_RESET)
                .build();

        OtpResponse response = OtpResponse.builder()
                .email("e****t@campuslink.io")
                .type(OtpType.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(600))
                .resendAvailableAt(Instant.now().plusSeconds(60))
                .build();

        when(otpService.generateAndSend(eq("etudiant@campuslink.io"), eq(OtpType.PASSWORD_RESET)))
                .thenReturn(response);

        mockMvc.perform(post("/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("PASSWORD_RESET"));
    }

    // ===================== POST /otp/verify =====================

    @Test
    void verify_shouldReturn200_whenCodeValid() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("etudiant@campuslink.io")
                .code("123456")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verify_shouldReturn400_whenCodeIncorrect() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("etudiant@campuslink.io")
                .code("000000")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        doThrow(new InvalidOtpException("Code incorrect."))
                .when(otpService).verify("etudiant@campuslink.io", "000000", OtpType.EMAIL_VERIFICATION);

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Code incorrect."));
    }

    @Test
    void verify_shouldReturn400_whenCodeFormatInvalid() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("etudiant@campuslink.io")
                .code("abc")
                .type(OtpType.EMAIL_VERIFICATION)
                .build();

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
