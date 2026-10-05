package com.campuslink.dto.request;

import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Critères de recherche dynamique d'utilisateurs (module Administration).
 * Tous les champs sont optionnels — seuls ceux renseignés participent à la
 * requête (voir {@code UserSpecification.withCriteria}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchCriteria {

    private String email;
    private AccountStatus status;
    private RoleName role;
    private Boolean emailVerified;

}
