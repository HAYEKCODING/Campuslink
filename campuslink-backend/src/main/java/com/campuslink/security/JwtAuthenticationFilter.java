package com.campuslink.security;

import com.campuslink.constant.SecurityConstants;
import com.campuslink.util.EmailMasker;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre exécuté une fois par requête ({@link OncePerRequestFilter}), chargé
 * d'authentifier la requête à partir d'un access token JWT.
 *
 * <p><strong>Stratégie retenue</strong> : à chaque requête porteuse d'un token
 * valide, l'utilisateur est <strong>rechargé depuis la base</strong> via
 * {@link UserDetailsService} (implémenté par {@link CustomUserDetailsService}),
 * plutôt que de reconstruire l'identité uniquement à partir des claims du token.
 * Coût : un accès DB par requête authentifiée. Bénéfice : un changement de statut
 * de compte (bannissement, suspension) ou de rôle est appliqué immédiatement,
 * sans attendre l'expiration naturelle du token.</p>
 *
 * <p>En cas de token absent, invalide, expiré ou de type "REFRESH" (un refresh
 * token ne doit jamais authentifier une requête vers une ressource protégée),
 * le filtre laisse simplement la requête continuer <em>sans</em> authentification :
 * c'est ensuite {@link SecurityConfig} (règles d'autorisation) et
 * {@link JwtAuthenticationEntryPoint} (réponse 401) qui prennent le relais.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Tente d'authentifier la requête à partir du token. Toute erreur (signature
     * invalide, token expiré, type incorrect, utilisateur introuvable) est
     * interceptée et journalisée : la requête repart simplement non authentifiée
     * plutôt que de faire échouer la chaîne de filtres.
     */
    private void authenticate(String token, HttpServletRequest request) {
        try {
            if (!jwtService.isAccessToken(token)) {
                log.debug("Token JWT rejeté : ce n'est pas un access token.");
                return;
            }

            String email = jwtService.extractUsername(token);
            if (!StringUtils.hasText(email)) {
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                log.debug("Token JWT invalide ou expiré pour l'utilisateur : {}", EmailMasker.mask(email));
                return;
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } catch (JwtException ex) {
            log.debug("Échec de lecture du token JWT : {}", ex.getMessage());
        } catch (UsernameNotFoundException ex) {
            log.debug("Utilisateur du token introuvable en base : {}", ex.getMessage());
        }
    }

    /**
     * Extrait le token brut de l'en-tête {@code Authorization: Bearer <token>}.
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

}
