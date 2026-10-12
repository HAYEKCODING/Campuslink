package com.campuslink.realtime.controller;

import com.campuslink.realtime.config.WebRtcProperties;
import com.campuslink.realtime.dto.response.WebRtcConfigurationResponse;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/matches/{matchId}/calls")
@RequiredArgsConstructor
public class CallConfigurationController {
    private final WebRtcProperties properties;
    private final MatchService matchService;
    private final CurrentUserPort currentUserPort;

    @GetMapping("/ice-servers")
    public WebRtcConfigurationResponse getIceServers(@PathVariable Long matchId) {
        Match match = matchService.getMatchOuException(matchId);
        matchService.verifierParticipant(match, currentUserPort.getCurrentUserId());
        if (match.getStatut() != MatchStatus.ACTIF) {
            throw new DuplicateResourceException("Les appels sont indisponibles pour cette conversation.");
        }

        List<WebRtcConfigurationResponse.IceServer> iceServers = new ArrayList<>();
        List<String> stunUrls = urls(properties.getStunUrls());
        if (!stunUrls.isEmpty()) {
            iceServers.add(WebRtcConfigurationResponse.IceServer.builder().urls(stunUrls).build());
        }

        List<String> turnUrls = urls(properties.getTurnUrls());
        if (!turnUrls.isEmpty() && properties.getTurnSharedSecret() != null
                && !properties.getTurnSharedSecret().isBlank()) {
            long expiry = Instant.now().getEpochSecond()
                    + Math.max(300, Math.min(properties.getTurnCredentialTtlSeconds(), 86_400));
            String username = expiry + ":" + currentUserPort.getCurrentUserId();
            iceServers.add(WebRtcConfigurationResponse.IceServer.builder()
                    .urls(turnUrls)
                    .username(username)
                    .credential(createTurnCredential(username, properties.getTurnSharedSecret()))
                    .build());
        }
        return WebRtcConfigurationResponse.builder().iceServers(iceServers).build();
    }

    private List<String> urls(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(value.split(",")).stream().map(String::trim).filter(url -> !url.isBlank()).toList();
    }

    private String createTurnCredential(String username, String sharedSecret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(hmac.doFinal(username.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Impossible de générer les identifiants TURN temporaires.", ex);
        }
    }
}
