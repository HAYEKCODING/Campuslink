package com.campuslink.dto.request;

import com.campuslink.enums.Gender;
import lombok.AllArgsConstructor;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Critères de recherche dynamique de profils.
 *
 * <p>Tous les champs sont optionnels — seuls ceux renseignés participent à
 * la requête (voir {@code ProfileSpecification.withCriteria}). Volontairement
 * dépourvu d'annotations Bean Validation : les contraintes (bornes d'âge
 * réalistes, etc.) sont portées directement sur les {@code @RequestParam} du
 * controller, là où arrivent les valeurs brutes envoyées par le client — ce
 * DTO n'est qu'un objet de transport interne entre controller et service.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileSearchCriteria {

    private Integer minAge;
    private Integer maxAge;
    private Gender gender;
    private String university;
    private String fieldOfStudy;
    private String neighborhood;
    private String city;
    private Set<String> interests;

    /**
     * Identifiant de l'utilisateur à exclure des résultats (son propre profil).
     * Renseigné depuis {@code GET /profiles/search} quand l'appelant est
     * authentifié : on ne voit pas soi-même dans le feed de découverte ni dans
     * la recherche — liker son propre profil est de toute façon rejeté par le
     * module temps réel ({@code LikeServiceImpl}). {@code null} = aucun filtre
     * (appel anonyme).
     */
    private UUID excludedUserId;

}
