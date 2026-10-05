package com.campuslink.service;

import com.campuslink.dto.request.AdminUpdateUserRequest;
import com.campuslink.dto.request.UserSearchCriteria;
import com.campuslink.dto.response.AdminStatsResponse;
import com.campuslink.dto.response.AdminUserResponse;
import com.campuslink.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Contrat métier du module Administration.
 *
 * <p>Plusieurs méthodes reçoivent {@code actingAdminId} : l'identifiant de
 * l'administrateur qui effectue l'action, nécessaire pour empêcher un admin
 * de se supprimer, se suspendre, ou retirer son propre rôle ADMIN (voir les
 * Javadoc de chaque méthode concernée).</p>
 */
public interface AdminUserService {

    Page<AdminUserResponse> listUsers(UserSearchCriteria criteria, Pageable pageable);

    AdminUserResponse updateUser(UUID userId, AdminUpdateUserRequest request);

    void deleteUser(UUID userId, UUID actingAdminId);

    AdminUserResponse suspendUser(UUID userId, UUID actingAdminId);

    AdminUserResponse activateUser(UUID userId);

    AdminUserResponse changeRole(UUID userId, RoleName newRole, UUID actingAdminId);

    AdminStatsResponse getStatistics();

}
