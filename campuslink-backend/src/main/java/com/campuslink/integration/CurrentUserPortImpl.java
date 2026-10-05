package com.campuslink.integration;

import com.campuslink.exception.UnauthorizedException;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.repository.UserRepository;
import com.campuslink.security.UserPrincipal;
import com.campuslink.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implémentation du {@code CurrentUserPort} du module {@code realtime} (DevC),
 * adossée au {@code SecurityContext} JWT du backend principal.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserPortImpl implements CurrentUserPort {

    private final UserRepository userRepository;

    @Override
    public Long getCurrentUserId() {
        return currentPrincipal().getUser().getLegacyId();
    }

    @Override
    public String getCurrentUserRole() {
        return currentPrincipal().getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
    }

    @Override
    public boolean isUtilisateurActif(Long userId) {
        return userRepository.findByLegacyId(userId)
                .map(u -> u.getStatus() == AccountStatus.ACTIVE || u.getStatus() == AccountStatus.PENDING_VERIFICATION)
                .orElse(false);
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Utilisateur non authentifié.");
        }
        return principal;
    }

}
