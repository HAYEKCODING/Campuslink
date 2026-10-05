package com.campuslink.dto.response;

import com.campuslink.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Représentation publique d'un utilisateur — ne porte jamais le mot de passe
 * (même hashé) ni aucune information de sécurité sensible.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String email;
    private AccountStatus status;
    private boolean emailVerified;
    private Set<String> roles;
    private Instant createdAt;

}
