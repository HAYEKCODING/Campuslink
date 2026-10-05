package com.campuslink.security;

import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur entre l'entité métier {@link User} et le contrat {@link UserDetails}
 * attendu par Spring Security.
 *
 * <p>Volontairement distinct de {@code org.springframework.security.core.userdetails.User}
 * (l'implémentation par défaut) : envelopper notre propre entité permet de garder
 * un accès direct à {@link #getUser()} (id, statut, etc.) plus loin dans la
 * chaîne de traitement — par exemple pour construire une réponse d'API sans
 * requête supplémentaire.</p>
 *
 * <p>Le mapping {@link AccountStatus} → indicateurs {@code UserDetails} centralise
 * ici la politique de compte : c'est le seul endroit du code où l'on décide
 * qu'un statut donné bloque ou non l'authentification.</p>
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    public UUID getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toSet());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * L'email sert d'identifiant unique ("username") pour Spring Security.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        // Aucune politique d'expiration de compte pour l'instant.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Un compte suspendu ou banni est considéré comme verrouillé.
        return user.getStatus() != AccountStatus.SUSPENDED
                && user.getStatus() != AccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Aucune politique d'expiration de mot de passe pour l'instant.
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Un compte désactivé (par l'utilisateur) ou banni ne peut plus s'authentifier.
        // Note : l'exigence "email vérifié pour se connecter" relève de la logique
        // métier du futur AuthService, pas de ce contrat technique Spring Security.
        return user.getStatus() != AccountStatus.DEACTIVATED
                && user.getStatus() != AccountStatus.BANNED;
    }

}
