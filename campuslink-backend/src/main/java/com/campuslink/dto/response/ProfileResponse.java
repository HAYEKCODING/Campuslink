package com.campuslink.dto.response;

import com.campuslink.enums.Gender;
import com.campuslink.enums.StudyLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Représentation d'un profil, utilisée à la fois pour "mon profil"
 * ({@code GET /profiles/me}) et pour un profil public
 * ({@code GET /profiles/{id}/public}) — même forme dans les deux cas,
 * conformément au périmètre de champs défini pour ce module.
 *
 * <p>{@code dateOfBirth} est renseignée uniquement par le point d'accès privé
 * {@code GET /profiles/me}; les vues publiques ne l'exposent pas.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private UUID id;

    /**
     * Identifiant numérique de l'utilisateur (voir {@code User.legacyId}) —
     * c'est celui-ci qu'il faut utiliser pour liker/matcher/contacter cette
     * personne (module {@code realtime}), pas l'UUID {@code id} ci-dessus.
     */
    private Long legacyId;

    private String avatarUrl;
    private String firstName;
    private String lastName;
    private Integer age;
    private LocalDate dateOfBirth;
    private Gender gender;
    private StudyLevel level;
    private String university;
    private String fieldOfStudy;
    private String neighborhood;
    private String city;
    private String bio;
    private Set<String> interests;

}
