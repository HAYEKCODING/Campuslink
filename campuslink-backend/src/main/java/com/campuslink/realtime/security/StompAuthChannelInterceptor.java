package com.campuslink.realtime.security;

import com.campuslink.entity.User;
import com.campuslink.repository.UserRepository;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Valide le JWT lors de la trame STOMP CONNECT et attache un Principal à la
 * session WebSocket. Réutilise le {@link JwtService} JWT du backend principal
 * (même secret, même format de token que l'authentification HTTP) — le Principal
 * STOMP porte l'id numérique legacy de l'utilisateur ({@code User.legacyId}),
 * seul identifiant attendu par le module {@code realtime} (DevC).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resoudreToken(accessor);

            if (token == null) {
                log.warn("Connexion WebSocket refusee : token JWT absent");
                throw new IllegalArgumentException("Token JWT manquant pour la connexion WebSocket");
            }

            try {
                if (!jwtService.isAccessToken(token)) {
                    throw new IllegalArgumentException("Le token fourni n'est pas un access token valide");
                }

                String email = jwtService.extractUsername(token);
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable pour ce token"));

                UserPrincipal userPrincipal = new UserPrincipal(user);
                if (!jwtService.isTokenValid(token, userPrincipal)) {
                    throw new IllegalArgumentException("Token JWT invalide ou expire");
                }

                if (user.getLegacyId() == null) {
                    throw new IllegalArgumentException("Utilisateur non éligible au module temps réel (legacyId absent)");
                }

                Principal principal = new UsernamePasswordAuthenticationToken(
                        user.getLegacyId().toString(), null, userPrincipal.getAuthorities());
                accessor.setUser(principal);
                log.info("Connexion WebSocket authentifiee pour l'utilisateur legacyId={}", user.getLegacyId());
            } catch (JwtException | IllegalArgumentException ex) {
                log.warn("Connexion WebSocket refusee : {}", ex.getMessage());
                throw new IllegalArgumentException("Token JWT invalide ou expire", ex);
            }
        }

        return message;
    }

    private String resoudreToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return accessor.getFirstNativeHeader("token");
    }
}
