package com.campuslink.controller;

import com.campuslink.dto.request.AdminUpdateUserRequest;
import com.campuslink.dto.request.ChangeRoleRequest;
import com.campuslink.dto.request.UserSearchCriteria;
import com.campuslink.dto.response.AdminStatsResponse;
import com.campuslink.dto.response.AdminUserResponse;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.PageResponse;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints du module Administration — gestion des utilisateurs.
 *
 * <p><strong>Protection ROLE_ADMIN à deux niveaux</strong> : {@code /admin/**}
 * est déjà restreint par {@code SecurityConfig} (règle de chemin, appliquée
 * au niveau du filtre de sécurité, avant même que la requête n'atteigne ce
 * controller). {@code @PreAuthorize} est répété ici <strong>à la classe <em>et</em>
 * à chaque méthode</strong> : seconde couche explicite et auto-documentée — si la
 * règle de chemin (ou l'annotation de classe) venait à être mal reconfigurée,
 * chaque méthode reste protégée individuellement. La redondance méthode par
 * méthode rend aussi la protection détectable par les scanners de sécurité
 * (qui ne savent pas résoudre les annotations de classe).</p>
 *
 * <p>Toutes les actions destructives ou limitant l'accès (suppression,
 * suspension, changement de rôle) reçoivent l'identifiant de l'administrateur
 * agissant, pour empêcher qu'il n'agisse sur son propre compte — voir
 * {@code AdminUserServiceImpl}.</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Gestion des utilisateurs — réservé aux administrateurs")
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lister et rechercher des utilisateurs",
            description = "Sans filtre : liste complète paginée. Avec filtres : recherche dynamique. "
                    + "Filtres combinables : email (contient), status, role, emailVerified."
    )
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @Parameter(description = "Email (recherche partielle, insensible à la casse)")
            @RequestParam(required = false) String email,

            @Parameter(description = "Statut du compte")
            @RequestParam(required = false) AccountStatus status,

            @Parameter(description = "Rôle")
            @RequestParam(required = false) RoleName role,

            @Parameter(description = "Email vérifié ou non")
            @RequestParam(required = false) Boolean emailVerified,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .email(email)
                .status(status)
                .role(role)
                .emailVerified(emailVerified)
                .build();

        Page<AdminUserResponse> results = adminUserService.listUsers(criteria, pageable);
        return ApiResponse.success(PageResponse.from(results));
    }

    @PatchMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier les champs administratifs d'un utilisateur (email, statut de vérification)")
    public ApiResponse<AdminUserResponse> updateUser(@PathVariable UUID userId,
                                                       @Valid @RequestBody AdminUpdateUserRequest request) {
        AdminUserResponse response = adminUserService.updateUser(userId, request);
        return ApiResponse.success(response, "Utilisateur mis à jour avec succès.");
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur", description = "Un administrateur ne peut pas se supprimer lui-même.")
    public ApiResponse<Void> deleteUser(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable UUID userId) {
        adminUserService.deleteUser(userId, principal.getId());
        return ApiResponse.success(null, "Utilisateur supprimé avec succès.");
    }

    @PatchMapping("/users/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Suspendre un utilisateur",
            description = "Révoque également toutes ses sessions actives. Un administrateur ne peut pas se suspendre lui-même."
    )
    public ApiResponse<AdminUserResponse> suspendUser(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable UUID userId) {
        AdminUserResponse response = adminUserService.suspendUser(userId, principal.getId());
        return ApiResponse.success(response, "Utilisateur suspendu avec succès.");
    }

    @PatchMapping("/users/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réactiver un utilisateur")
    public ApiResponse<AdminUserResponse> activateUser(@PathVariable UUID userId) {
        AdminUserResponse response = adminUserService.activateUser(userId);
        return ApiResponse.success(response, "Utilisateur réactivé avec succès.");
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Changer le rôle d'un utilisateur",
            description = "Remplace l'intégralité des rôles de l'utilisateur par celui fourni. "
                    + "Un administrateur ne peut pas retirer son propre rôle ADMIN."
    )
    public ApiResponse<AdminUserResponse> changeRole(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable UUID userId,
                                                       @Valid @RequestBody ChangeRoleRequest request) {
        AdminUserResponse response = adminUserService.changeRole(userId, request.getRole(), principal.getId());
        return ApiResponse.success(response, "Rôle mis à jour avec succès.");
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques globales sur les utilisateurs")
    public ApiResponse<AdminStatsResponse> getStatistics() {
        AdminStatsResponse response = adminUserService.getStatistics();
        return ApiResponse.success(response);
    }

}
