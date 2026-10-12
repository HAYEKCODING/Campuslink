package com.campuslink.security;

import com.campuslink.dto.response.OAuthLoginResult;
import com.campuslink.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;

    @Value("${application.oauth2.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oauthUser.getAttributes();

        String providerId = value(attributes.get(provider.equals("google") ? "sub" : "id"));
        String email = value(attributes.get("email"));
        String firstName = value(attributes.get("given_name"));
        String lastName = value(attributes.get("family_name"));
        if (provider.equals("facebook")) {
            firstName = value(attributes.get("first_name"));
            lastName = value(attributes.get("last_name"));
            if ((firstName == null || firstName.isBlank()) && attributes.get("name") instanceof String name) {
                String[] parts = name.trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            }
        }
        if ((firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank())
                && attributes.get("name") instanceof String name && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }
        boolean emailVerified = provider.equals("google")
                ? Boolean.TRUE.equals(attributes.get("email_verified"))
                : email != null && !email.isBlank();

        try {
            OAuthLoginResult result = authServiceProvider.getObject().loginWithOAuth(
                    provider, providerId, email, firstName, lastName, emailVerified);
            String tokens = "access_token=" + encode(result.getAuthResponse().getAccessToken())
                    + "&refresh_token=" + encode(result.getAuthResponse().getRefreshToken())
                    + "&new_account=" + result.isNewAccount();
            response.sendRedirect(frontendUrl.replaceAll("/+$", "") + "/oauth/callback#" + tokens);
        } catch (RuntimeException exception) {
            log.warn("Échec de finalisation OAuth pour le fournisseur {}: {}", provider, exception.getMessage());
            response.sendRedirect(frontendUrl.replaceAll("/+$", "") + "/connexion?oauthError=account");
        }
    }

    private String value(Object attribute) {
        return attribute instanceof String string ? string : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
