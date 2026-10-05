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
 * Vue d'un utilisateur destinée à l'administration — distincte de
 * {@link UserResponse} (utilisée notamment par la réponse d'inscription) :
 * expose en plus {@code lastLoginAt}, pertinent pour un admin, pas pour
 * l'utilisateur lui-même à ce stade de son parcours.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {

    private UUID id;
    private String email;
    private AccountStatus status;
    private boolean emailVerified;
    private Set<String> roles;
    private Instant createdAt;
    private Instant lastLoginAt;

}
