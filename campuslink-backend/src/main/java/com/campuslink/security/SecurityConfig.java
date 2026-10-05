package com.campuslink.security;

import com.campuslink.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration centrale de Spring Security 6.
 *
 * <p>Politique appliquée :</p>
 * <ul>
 *     <li>API stateless (aucune session HTTP, authentification 100% JWT)</li>
 *     <li>CSRF désactivé (non pertinent pour une API stateless consommée par des clients JWT
 *         plutôt que par un navigateur avec cookies de session)</li>
 *     <li>CORS délégué à {@link com.campuslink.config.CorsConfig}</li>
 *     <li>Mots de passe hashés via BCrypt (facteur de coût par défaut : 10)</li>
 *     <li>Authentification déléguée à {@link AuthenticationProvider} (DaoAuthenticationProvider),
 *         lui-même adossé à {@link CustomUserDetailsService}</li>
 *     <li>Endpoints publics définis dans {@link SecurityConstants#PUBLIC_ENDPOINTS} — tout le reste
 *         exige un access token JWT valide</li>
 *     <li>{@code /admin/**} exige en plus le rôle {@code ADMIN}, appliqué à <strong>deux niveaux</strong> :
 *         cette règle de chemin (filtre de sécurité) et {@code @PreAuthorize("hasRole('ADMIN')")}
 *         sur {@code AdminController} lui-même (activé par {@link EnableMethodSecurity}) — défense
 *         en profondeur, la seconde couche protège même si la première venait à être mal configurée</li>
 *     <li>Filtre {@link JwtAuthenticationFilter} inséré avant {@link UsernamePasswordAuthenticationFilter}
 *         pour peupler le contexte de sécurité avant toute tentative d'authentification par formulaire</li>
 *     <li>Réponses d'erreur JSON homogènes via {@link JwtAuthenticationEntryPoint} (401)
 *         et {@link JwtAccessDeniedHandler} (403)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Définit la chaîne de filtres de sécurité appliquée à toutes les requêtes HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS : la politique (origines autorisées, méthodes, headers) est définie
                // dans CorsConfig ; on se contente ici de l'activer sur la chaîne de filtres.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // CSRF désactivé : une API stateless authentifiée par Bearer token n'est pas
                // exposée aux attaques CSRF classiques (pas de cookie de session envoyé
                // automatiquement par le navigateur).
                .csrf(AbstractHttpConfigurer::disable)

                // Aucune session HTTP créée ni utilisée : chaque requête est authentifiée
                // indépendamment via son token JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Gestion homogène des erreurs de sécurité (réponses JSON, pas de redirection
                // vers une page de login qui n'existe pas côté API).
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // Règles d'autorisation, évaluées dans l'ordre : routes publiques, puis
                // routes d'administration (rôle ADMIN requis), puis tout le reste (authentifié
                // suffit). SecurityConstants.PUBLIC_ENDPOINTS ne contient jamais "/admin/**".
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SecurityConstants.ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .anyRequest().authenticated())

                // Le filtre JWT s'exécute avant le filtre d'authentification par formulaire
                // standard de Spring Security (que nous n'utilisons pas, mais qui reste
                // présent dans la chaîne par défaut).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Encodeur de mots de passe basé sur BCrypt.
     *
     * <p>BCrypt intègre nativement un sel aléatoire par mot de passe et un facteur
     * de travail configurable (10 par défaut ici), le rendant résistant aux
     * attaques par force brute même en cas de fuite de la base.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournisseur d'authentification : vérifie les identifiants (email + mot de passe)
     * en s'appuyant sur {@link CustomUserDetailsService} pour charger l'utilisateur et
     * sur {@link #passwordEncoder()} pour comparer le mot de passe fourni au hash stocké.
     *
     * <p>Ce bean est automatiquement détecté et intégré par Spring Security lors de la
     * construction de l'{@link AuthenticationManager} (voir {@link #authenticationManager}
     * ci-dessous) — aucun câblage manuel supplémentaire n'est nécessaire.</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expose l'{@link AuthenticationManager} en tant que bean Spring, afin qu'il puisse
     * être injecté dans le futur {@code AuthController} pour authentifier les tentatives
     * de connexion : {@code authenticationManager.authenticate(...)}.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}
