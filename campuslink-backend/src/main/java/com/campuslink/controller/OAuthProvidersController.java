package com.campuslink.controller;

import com.campuslink.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OAuthProvidersController {

    private final boolean googleEnabled;
    private final boolean facebookEnabled;

    public OAuthProvidersController(
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret,
            @Value("${FACEBOOK_CLIENT_ID:}") String facebookClientId,
            @Value("${FACEBOOK_CLIENT_SECRET:}") String facebookClientSecret) {
        this.googleEnabled = configured(googleClientId, googleClientSecret);
        this.facebookEnabled = configured(facebookClientId, facebookClientSecret);
    }

    @GetMapping("/providers")
    public ApiResponse<Map<String, Boolean>> providers() {
        return ApiResponse.success(
                Map.of("google", googleEnabled, "facebook", facebookEnabled),
                "Fournisseurs OAuth disponibles.");
    }

    private boolean configured(String clientId, String clientSecret) {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
