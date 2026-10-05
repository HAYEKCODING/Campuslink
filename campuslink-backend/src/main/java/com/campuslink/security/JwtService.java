package com.campuslink.security;

import com.campuslink.config.JwtProperties;
import com.campuslink.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service central de fabrication et de lecture des tokens JWT (access + refresh).
 *
 * <p>Conçu pour être indépendant de toute entité métier : il ne manipule que le
 * contrat {@link UserDetails} de Spring Security, ce qui le rend réutilisable
 * quelle que soit l'implémentation de {@code UserDetailsService} en place.</p>
 *
 * <p><strong>Ce que ce service NE fait PAS</strong> : il ne vérifie jamais lui-même
 * qu'un utilisateur a le droit de se connecter (rôle de {@code AuthenticationProvider})
 * ni ne persiste de token. Il transforme un {@link UserDetails} en JWT signé, et
 * inversement, un JWT en informations exploitables.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // ===================== Génération =====================

    /**
     * Génère un access token (courte durée de vie) pour l'utilisateur donné.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtProperties.getAccessTokenExpirationMs(),
                SecurityConstants.ACCESS_TOKEN_TYPE);
    }

    /**
     * Génère un refresh token (longue durée de vie) pour l'utilisateur donné.
     *
     * <p>Ne porte volontairement pas les autorités : un refresh token ne sert
     * qu'à obtenir un nouvel access token, jamais à autoriser un accès direct
     * à une ressource protégée (voir le contrôle de type dans
     * {@link JwtAuthenticationFilter}).</p>
     *
     * <p>Le paramètre {@code tokenId} est embarqué comme claim JWT standard
     * {@code jti} (JWT ID). Il correspond à l'identifiant technique de
     * l'enregistrement {@link com.campuslink.entity.RefreshToken} créé en base
     * par le service appelant, et permet de révoquer ce token précis avant
     * son expiration naturelle (logout, rotation lors d'un rafraîchissement).</p>
     */
    public String generateRefreshToken(UserDetails userDetails, UUID tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(tokenId.toString())
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getRefreshTokenExpirationMs())))
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, SecurityConstants.REFRESH_TOKEN_TYPE)
                .signWith(signingKey())
                .compact();
    }

    private String buildToken(UserDetails userDetails, long expirationMs, String tokenType) {
        Instant now = Instant.now();

        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.TOKEN_TYPE_CLAIM, tokenType);
        // Les autorités sont embarquées à titre informatif (lecture rapide côté
        // client pour adapter l'UI). Le serveur ne s'y fie jamais pour autoriser
        // une action : voir JwtAuthenticationFilter, qui recharge l'utilisateur
        // depuis la base via CustomUserDetailsService à chaque requête.
        claims.put(SecurityConstants.AUTHORITIES_CLAIM, authorities);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey())
                .compact();
    }

    // ===================== Lecture / extraction =====================

    /**
     * Extrait le "subject" du token, c'est-à-dire l'email de l'utilisateur.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait l'identifiant technique (claim {@code jti}) porté par un refresh
     * token — correspond à l'id de l'enregistrement {@link com.campuslink.entity.RefreshToken}
     * en base. Lève {@link IllegalArgumentException} si le claim est absent ou
     * n'est pas un UUID valide.
     */
    public UUID extractTokenId(String token) {
        String id = extractClaim(token, Claims::getId);
        if (id == null) {
            throw new IllegalArgumentException("Le token ne porte aucun identifiant (claim jti).");
        }
        return UUID.fromString(id);
    }

    /**
     * Extrait une claim arbitraire du token via une fonction de résolution.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ===================== Validation =====================

    /**
     * Valide un token vis-à-vis d'un {@link UserDetails} précis : le "subject"
     * du token doit correspondre à son username, et le token ne doit pas être expiré.
     *
     * <p>La signature est implicitement vérifiée en amont : {@link #extractUsername}
     * lève une {@link JwtException} (non catchée ici, propagée à l'appelant) si la
     * signature est invalide ou le token malformé.</p>
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Indique si le token porte le claim de type "ACCESS".
     * Un refresh token présenté sur un endpoint protégé doit être rejeté.
     */
    public boolean isAccessToken(String token) {
        return SecurityConstants.ACCESS_TOKEN_TYPE.equals(
                extractClaim(token, claims -> claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class)));
    }

    /**
     * Indique si le token porte le claim de type "REFRESH".
     * Utile pour le futur endpoint de rafraîchissement de token.
     */
    public boolean isRefreshToken(String token) {
        return SecurityConstants.REFRESH_TOKEN_TYPE.equals(
                extractClaim(token, claims -> claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class)));
    }

    // ===================== Métadonnées =====================

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

}
