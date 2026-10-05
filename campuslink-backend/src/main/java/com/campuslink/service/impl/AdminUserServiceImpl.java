package com.campuslink.service.impl;

import com.campuslink.dto.request.AdminUpdateUserRequest;
import com.campuslink.dto.request.UserSearchCriteria;
import com.campuslink.dto.response.AdminStatsResponse;
import com.campuslink.dto.response.AdminUserResponse;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.exception.BadRequestException;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.mapper.UserMapper;
import com.campuslink.repository.RefreshTokenRepository;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.AdminUserService;
import com.campuslink.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du module Administration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(UserSearchCriteria criteria, Pageable pageable) {
        Specification<User> specification = UserSpecification.withCriteria(criteria);
        Page<User> users = userRepository.findAll(specification, pageable);
        return users.map(userMapper::toAdminUserResponse);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUpdateUserRequest request) {
        User user = getUserOrThrow(userId);

        if (StringUtils.hasText(request.getEmail())) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new DuplicateResourceException("Un compte existe déjà avec l'email : " + normalizedEmail);
            }
            user.setEmail(normalizedEmail);
        }

        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }

        User saved = userRepository.save(user);
        log.info("Utilisateur {} modifié par un administrateur.", userId);

        return userMapper.toAdminUserResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, UUID actingAdminId) {
        ensureNotActingOnSelf(userId, actingAdminId, "supprimer");

        User user = getUserOrThrow(userId);
        userRepository.delete(user);

        log.info("Utilisateur {} supprimé par l'administrateur {}.", userId, actingAdminId);
    }

    @Override
    @Transactional
    public AdminUserResponse suspendUser(UUID userId, UUID actingAdminId) {
        ensureNotActingOnSelf(userId, actingAdminId, "suspendre");

        User user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.SUSPENDED);
        User saved = userRepository.save(user);

        int revokedCount = refreshTokenRepository.revokeAllActiveForUser(user, Instant.now());

        log.info("Utilisateur {} suspendu par l'administrateur {} — {} session(s) révoquée(s).",
                userId, actingAdminId, revokedCount);

        return userMapper.toAdminUserResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse activateUser(UUID userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(AccountStatus.ACTIVE);
        User saved = userRepository.save(user);

        log.info("Utilisateur {} activé par un administrateur.", userId);

        return userMapper.toAdminUserResponse(saved);
    }

    @Override
    @Transactional
    public AdminUserResponse changeRole(UUID userId, RoleName newRole, UUID actingAdminId) {
        if (userId.equals(actingAdminId) && newRole != RoleName.ADMIN) {
            throw new BadRequestException("Vous ne pouvez pas retirer votre propre rôle ADMIN.");
        }

        User user = getUserOrThrow(userId);
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle " + newRole + " introuvable en base — vérifier le script de seed (database/schema.sql)."));

        new HashSet<>(user.getRoles()).forEach(user::removeRole);
        user.addRole(role);

        User saved = userRepository.save(user);
        log.info("Rôle de l'utilisateur {} changé en {} par l'administrateur {}.", userId, newRole, actingAdminId);

        return userMapper.toAdminUserResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStatistics() {
        Map<String, Long> usersByStatus = Arrays.stream(AccountStatus.values())
                .collect(Collectors.toMap(Enum::name, userRepository::countByStatus));

        Map<String, Long> usersByRole = Arrays.stream(RoleName.values())
                .collect(Collectors.toMap(Enum::name, userRepository::countByRoles_Name));

        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .usersByStatus(usersByStatus)
                .usersByRole(usersByRole)
                .emailVerifiedCount(userRepository.countByEmailVerifiedTrue())
                .emailUnverifiedCount(userRepository.countByEmailVerifiedFalse())
                .newUsersLast7Days(userRepository.countByCreatedAtAfter(sevenDaysAgo))
                .newUsersLast30Days(userRepository.countByCreatedAtAfter(thirtyDaysAgo))
                .build();
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));
    }

    private void ensureNotActingOnSelf(UUID targetUserId, UUID actingAdminId, String action) {
        if (targetUserId.equals(actingAdminId)) {
            throw new BadRequestException("Vous ne pouvez pas " + action + " votre propre compte via ce module.");
        }
    }

}
