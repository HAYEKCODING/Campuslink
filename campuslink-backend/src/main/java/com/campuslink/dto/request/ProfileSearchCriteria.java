package com.campuslink.dto.request;

import lombok.AllArgsConstructor;
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
    private String university;
    private String fieldOfStudy;
    private String neighborhood;
    private String city;
    private Set<String> interests;

}
