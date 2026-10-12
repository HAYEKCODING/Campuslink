package com.campuslink.controller;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.enums.Gender;
import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.PageResponse;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.ProfileSearchService;
import com.campuslink.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/**
 * Endpoints du module Profile : création, modification, suppression,
 * consultation (privée et publique), et recherche dynamique de profils.
 *
 * <p>Les routes {@code /me} identifient l'utilisateur via le token JWT
 * ({@code @AuthenticationPrincipal}) — jamais via un identifiant fourni par
 * le client, pour qu'un utilisateur ne puisse agir que sur son propre profil.
 * {@code GET /{profileId}/public} et {@code GET /search} sont accessibles
 * sans authentification (voir {@code SecurityConstants.PUBLIC_ENDPOINTS}) —
 * même logique que pour un profil individuel : la recherche n'expose rien
 * de plus que ce qui est déjà consultable profil par profil.</p>
 */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Profils", description = "Création, modification, suppression, consultation et recherche de profils")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileSearchService profileSearchService;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer le profil de l'utilisateur authentifié")
    public ApiResponse<ProfileResponse> createProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.createProfile(principal.getId(), request);
        return ApiResponse.success(response, "Profil créé avec succès.");
    }

    @PutMapping("/me")
    @Operation(summary = "Modifier le profil de l'utilisateur authentifié")
    public ApiResponse<ProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.updateProfile(principal.getId(), request);
        return ApiResponse.success(response, "Profil mis à jour avec succès.");
    }

    @DeleteMapping("/me")
    @Operation(summary = "Supprimer le profil de l'utilisateur authentifié")
    public ApiResponse<Void> deleteProfile(@AuthenticationPrincipal UserPrincipal principal) {
        profileService.deleteProfile(principal.getId());
        return ApiResponse.success(null, "Profil supprimé avec succès.");
    }

    @GetMapping("/me")
    @Operation(summary = "Afficher le profil de l'utilisateur authentifié")
    public ApiResponse<ProfileResponse> getOwnProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse response = profileService.getOwnProfile(principal.getId());
        return ApiResponse.success(response);
    }

    @GetMapping("/{profileId}/public")
    @Operation(summary = "Afficher le profil public d'un utilisateur", description = "Endpoint public, accessible sans authentification.")
    public ApiResponse<ProfileResponse> getPublicProfile(@PathVariable UUID profileId) {
        ProfileResponse response = profileService.getPublicProfile(profileId);
        return ApiResponse.success(response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Rechercher des profils par filtres dynamiques",
            description = "Endpoint public. Tous les filtres sont optionnels et combinables. "
                    + "Pagination et tri via les paramètres standards Spring Data : "
                    + "page, size, sort (ex. sort=age,desc ou sort=city,asc)."
    )
    public ApiResponse<PageResponse<ProfileResponse>> search(
            @Parameter(description = "Âge minimum (inclus)")
            @RequestParam(required = false) @Min(value = 0, message = "L'âge minimum ne peut pas être négatif.")
            @Max(value = 150, message = "L'âge minimum n'est pas réaliste.") Integer minAge,

            @Parameter(description = "Âge maximum (inclus)")
            @RequestParam(required = false) @Min(value = 0, message = "L'âge maximum ne peut pas être négatif.")
            @Max(value = 150, message = "L'âge maximum n'est pas réaliste.") Integer maxAge,

            @Parameter(description = "Genre (MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY)")
            @RequestParam(required = false) Gender gender,

            @Parameter(description = "Établissement (recherche partielle, insensible à la casse)")
            @RequestParam(required = false) String university,

            @Parameter(description = "Filière (recherche partielle, insensible à la casse)")
            @RequestParam(required = false) String fieldOfStudy,

            @Parameter(description = "Quartier (recherche partielle, insensible à la casse)")
            @RequestParam(required = false) String neighborhood,

            @Parameter(description = "Ville (recherche partielle, insensible à la casse)")
            @RequestParam(required = false) String city,

            @Parameter(description = "Centres d'intérêt — correspond aux profils ayant au moins un des intérêts listés")
            @RequestParam(required = false) Set<String> interests,

            @PageableDefault(size = 20, sort = "firstName") Pageable pageable,

            @Parameter(description = "Utilisateur connecté — son propre profil est exclu des résultats (null si anonyme)")
            @AuthenticationPrincipal UserPrincipal principal) {

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .minAge(minAge)
                .maxAge(maxAge)
                .gender(gender)
                .university(university)
                .fieldOfStudy(fieldOfStudy)
                .neighborhood(neighborhood)
                .city(city)
                .interests(interests)
                .excludedUserId(principal != null ? principal.getId() : null)
                .build();

        var results = profileSearchService.search(criteria, pageable);
        return ApiResponse.success(PageResponse.from(results));
    }

}
