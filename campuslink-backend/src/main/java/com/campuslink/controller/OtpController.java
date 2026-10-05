package com.campuslink.controller;

import com.campuslink.dto.request.OtpRequest;
import com.campuslink.dto.request.VerifyOtpRequest;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.OtpResponse;
import com.campuslink.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints du module OTP : envoi, renvoi et vérification de codes à usage unique.
 *
 * <p>{@code /send} et {@code /resend} délèguent tous deux à
 * {@link OtpService#generateAndSend} — voir la Javadoc de cette méthode pour
 * la justification (le comportement anti-spam et anti-énumération est
 * identique quel que soit l'endpoint appelé).</p>
 */
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "Génération, renvoi et vérification de codes à usage unique")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    @Operation(summary = "Générer et envoyer un code OTP par email")
    public ApiResponse<OtpResponse> send(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = otpService.generateAndSend(request.getEmail(), request.getType());
        return ApiResponse.success(response, "Si un compte existe pour cet email, un code a été envoyé.");
    }

    @PostMapping("/resend")
    @Operation(summary = "Renvoyer un code OTP (soumis au même délai anti-spam que /send)")
    public ApiResponse<OtpResponse> resend(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = otpService.generateAndSend(request.getEmail(), request.getType());
        return ApiResponse.success(response, "Si un compte existe pour cet email, un nouveau code a été envoyé.");
    }

    @PostMapping("/verify")
    @Operation(summary = "Vérifier un code OTP")
    public ApiResponse<Void> verify(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verify(request.getEmail(), request.getCode(), request.getType());
        return ApiResponse.success(null, "Code vérifié avec succès.");
    }

}
