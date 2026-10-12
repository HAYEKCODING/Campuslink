package com.campuslink.realtime.security;

import com.campuslink.entity.User;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ForbiddenException;
import com.campuslink.repository.UserRepository;
import com.campuslink.realtime.entity.Match;
import com.campuslink.realtime.entity.enums.MatchStatus;
import com.campuslink.realtime.service.MatchService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern TYPING_DESTINATION = Pattern.compile("^/topic/matches/(\\d+)/typing$");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final MatchService matchService;

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

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null) {
                Matcher typingDestination = TYPING_DESTINATION.matcher(destination);
                if (typingDestination.matches()) {
                    Principal principal = accessor.getUser();
                    if (principal == null) {
                        throw new ForbiddenException("Authentification requise pour cette conversation");
                    }
                    Match match = matchService.getMatchOuException(Long.valueOf(typingDestination.group(1)));
                    matchService.verifierParticipant(match, Long.valueOf(principal.getName()));
                    if (match.getStatut() != MatchStatus.ACTIF) {
                        throw new DuplicateResourceException("Ce match n'est plus actif");
                    }
                }
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
