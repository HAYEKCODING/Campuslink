package com.campuslink.service.impl;

import com.campuslink.dto.request.LoginRequest;
import com.campuslink.dto.request.RefreshTokenRequest;
import com.campuslink.dto.request.RegisterRequest;
import com.campuslink.dto.response.AuthResponse;
import com.campuslink.dto.response.UserResponse;
import com.campuslink.entity.Profile;
import com.campuslink.entity.RefreshToken;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.OtpType;
import com.campuslink.enums.RoleName;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.UnauthorizedException;
import com.campuslink.mapper.UserMapper;
import com.campuslink.repository.RefreshTokenRepository;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.AuthService;
import com.campuslink.service.OtpService;
import com.campuslink.util.EmailMasker;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implémentation du module d'authentification.
 *
 * <p>Orchestre les composants de sécurité déjà en place ({@code AuthenticationManager},
 * {@code JwtService}, {@code CustomUserDetailsService} indirectement via
 * {@code AuthenticationProvider}) sans dupliquer leur logique — cette classe ne fait
 * que les combiner (principe SRP : chaque composant a une seule responsabilité,
 * celle-ci les orchestre).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final OtpService otpService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Un compte existe déjà avec l'email : " + email);
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle STUDENT introuvable en base — vérifier le script de seed (database/schema.sql)."));

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        user.addRole(studentRole);

        Profile profile = Profile.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .build();
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        log.info("Nouveau compte créé : {}", EmailMasker.mask(savedUser.getEmail()));

        // generateAndSend() est annotée REQUIRES_NEW côté OtpServiceImpl : un échec
        // d'envoi d'email (SMTP injoignable, etc.) tourne dans sa propre transaction
        // et ne peut donc pas marquer CETTE transaction (création du compte) comme
        // rollback-only. On journalise sans jamais faire échouer l'inscription.
        try {
            otpService.generateAndSend(savedUser.getEmail(), OtpType.EMAIL_VERIFICATION);
        } catch (Exception ex) {
            log.warn("Échec de l'envoi de l'email de vérification pour {} : {}",
                    EmailMasker.mask(savedUser.getEmail()), ex.getMessage());
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        // Délègue la vérification des identifiants à AuthenticationProvider
        // (DaoAuthenticationProvider -> CustomUserDetailsService + BCrypt).
        // Lève BadCredentialsException / DisabledException / LockedException,
        // toutes gérées par GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("Connexion réussie : {}", EmailMasker.mask(email));
        return issueTokens(principal);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtService.isRefreshToken(token)) {
            throw new UnauthorizedException("Le token fourni n'est pas un refresh token valide.");
        }

        UUID tokenId;
        String email;
        try {
            tokenId = jwtService.extractTokenId(token);
            email = jwtService.extractUsername(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Refresh token invalide ou expiré.");
        }

        RefreshToken storedToken = refreshTokenRepository.findByIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide, expiré ou déjà utilisé."));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expiré.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur associé au token introuvable."));

        // Rotation : le token présenté est révoqué immédiatement, qu'il soit
        // rejoué ou non par la suite — limite l'impact d'un token intercepté.
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        log.info("Rafraîchissement de token pour : {}", EmailMasker.mask(email));
        return issueTokens(new UserPrincipal(user));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        try {
            UUID tokenId = jwtService.extractTokenId(token);
            refreshTokenRepository.findByIdAndRevokedFalse(tokenId).ifPresent(storedToken -> {
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);
                log.info("Refresh token révoqué (logout) : {}", tokenId);
            });
        } catch (JwtException | IllegalArgumentException ex) {
            // Un token déjà invalide/malformé n'a rien à révoquer : opération
            // idempotente, on ne renvoie pas d'erreur au client pour un logout.
            log.debug("Logout avec un token déjà invalide : {}", ex.getMessage());
        }
    }

    /**
     * Émet une nouvelle paire de tokens pour l'utilisateur donné, en créant au
     * préalable l'enregistrement {@link RefreshToken} qui fournit l'identifiant
     * (claim {@code jti}) embarqué dans le refresh token.
     */
    private AuthResponse issueTokens(UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(principal.getUser())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .build();
        refreshTokenEntity = refreshTokenRepository.save(refreshTokenEntity);

        String refreshToken = jwtService.generateRefreshToken(principal, refreshTokenEntity.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .user(userMapper.toUserResponse(principal.getUser()))
                .build();
    }

    private String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase();
    }

}
