package com.campuslink.security;

import com.campuslink.entity.User;
import com.campuslink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du contrat {@link UserDetailsService} de Spring Security,
 * adossée à {@link UserRepository}.
 *
 * <p>Utilisée à deux moments distincts du cycle de vie de l'authentification :</p>
 * <ul>
 *     <li>Par {@code AuthenticationProvider} (voir {@link SecurityConfig}) lors
 *     de la vérification initiale des identifiants (login) ;</li>
 *     <li>Par {@link JwtAuthenticationFilter} à <strong>chaque requête</strong>
 *     porteuse d'un token, afin de recharger l'état courant de l'utilisateur
 *     depuis la base plutôt que de faire confiance aux claims du token.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Le paramètre {@code username} correspond ici à l'email de l'utilisateur
     * (voir {@link UserPrincipal#getUsername()}).</p>
     *
     * @throws UsernameNotFoundException si aucun utilisateur ne correspond à cet email.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé avec l'email : " + username));

        return new UserPrincipal(user);
    }

}
