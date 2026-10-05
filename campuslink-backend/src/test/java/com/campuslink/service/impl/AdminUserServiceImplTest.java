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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link AdminUserServiceImpl}, isolés de la base via
 * Mockito. Complémentaires à {@code UserSpecificationIntegrationTest} qui
 * vérifie le comportement réel des prédicats de recherche contre H2.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userRepository, roleRepository, refreshTokenRepository, userMapper);
    }

    // ===================== listUsers =====================

    @Test
    void listUsers_shouldReturnMappedPage() {
        User user = User.builder().email("awa@campuslink.io").password("hashed").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toAdminUserResponse(user))
                .thenReturn(AdminUserResponse.builder().email("awa@campuslink.io").build());

        Page<AdminUserResponse> result = adminUserService.listUsers(UserSearchCriteria.builder().build(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(AdminUserResponse::getEmail).containsExactly("awa@campuslink.io");
    }

    // ===================== updateUser =====================

    @Nested
    class UpdateUser {

        @Test
        void shouldUpdateEmailAndVerificationStatus() {
            User user = User.builder().email("ancien@campuslink.io").password("hashed").build();
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .email("nouveau@campuslink.io")
                    .emailVerified(true)
                    .build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("nouveau@campuslink.io")).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toAdminUserResponse(user))
                    .thenReturn(AdminUserResponse.builder().email("nouveau@campuslink.io").build());

            AdminUserResponse response = adminUserService.updateUser(TARGET_USER_ID, request);

            assertThat(user.getEmail()).isEqualTo("nouveau@campuslink.io");
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(response.getEmail()).isEqualTo("nouveau@campuslink.io");
        }

        @Test
        void shouldThrowDuplicateResource_whenNewEmailAlreadyTaken() {
            User user = User.builder().email("ancien@campuslink.io").password("hashed").build();
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().email("pris@campuslink.io").build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("pris@campuslink.io")).thenReturn(true);

            assertThatThrownBy(() -> adminUserService.updateUser(TARGET_USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowResourceNotFound_whenUserUnknown() {
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.updateUser(TARGET_USER_ID, AdminUpdateUserRequest.builder().build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ===================== deleteUser =====================

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUser_whenNotActingOnSelf() {
            User user = User.builder().email("awa@campuslink.io").password("hashed").build();
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

            adminUserService.deleteUser(TARGET_USER_ID, ADMIN_ID);

            verify(userRepository).delete(user);
        }

        @Test
        void shouldThrowBadRequest_whenAdminTargetsSelf() {
            assertThatThrownBy(() -> adminUserService.deleteUser(ADMIN_ID, ADMIN_ID))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).delete(any(User.class));
            verify(userRepository, never()).findById(any());
        }
    }

    // ===================== suspendUser =====================

    @Nested
    class SuspendUser {

        @Test
        void shouldSuspendUser_andRevokeActiveSessions() {
            User user = User.builder().email("awa@campuslink.io").password("hashed").build();

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(refreshTokenRepository.revokeAllActiveForUser(eq(user), any(Instant.class))).thenReturn(3);
            when(userMapper.toAdminUserResponse(user))
                    .thenReturn(AdminUserResponse.builder().status(AccountStatus.SUSPENDED).build());

            AdminUserResponse response = adminUserService.suspendUser(TARGET_USER_ID, ADMIN_ID);

            assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
            assertThat(response.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
            verify(refreshTokenRepository).revokeAllActiveForUser(eq(user), any(Instant.class));
        }

        @Test
        void shouldThrowBadRequest_whenAdminTargetsSelf() {
            assertThatThrownBy(() -> adminUserService.suspendUser(ADMIN_ID, ADMIN_ID))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).findById(any());
        }
    }

    // ===================== activateUser =====================

    @Test
    void activateUser_shouldSetStatusActive() {
        User user = User.builder().email("awa@campuslink.io").password("hashed").status(AccountStatus.SUSPENDED).build();

        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toAdminUserResponse(user))
                .thenReturn(AdminUserResponse.builder().status(AccountStatus.ACTIVE).build());

        AdminUserResponse response = adminUserService.activateUser(TARGET_USER_ID);

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    // ===================== changeRole =====================

    @Nested
    class ChangeRole {

        @Test
        void shouldReplaceAllRoles_withTheSingleRoleProvided() {
            Role studentRole = Role.builder().name(RoleName.STUDENT).build();
            Role teacherRole = Role.builder().name(RoleName.TEACHER).build();

            User user = User.builder().email("awa@campuslink.io").password("hashed").build();
            user.addRole(studentRole);

            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(teacherRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toAdminUserResponse(user))
                    .thenReturn(AdminUserResponse.builder().roles(Set.of("TEACHER")).build());

            AdminUserResponse response = adminUserService.changeRole(TARGET_USER_ID, RoleName.TEACHER, ADMIN_ID);

            assertThat(user.getRoles()).containsExactly(teacherRole);
            assertThat(response.getRoles()).containsExactly("TEACHER");
        }

        @Test
        void shouldThrowBadRequest_whenAdminRemovesOwnAdminRole() {
            assertThatThrownBy(() -> adminUserService.changeRole(ADMIN_ID, RoleName.STUDENT, ADMIN_ID))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).findById(any());
        }

        @Test
        void shouldAllow_whenAdminKeepsOwnAdminRole() {
            Role adminRole = Role.builder().name(RoleName.ADMIN).build();
            User admin = User.builder().email("admin@campuslink.io").password("hashed").build();
            admin.addRole(adminRole);

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
            when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(admin)).thenReturn(admin);
            when(userMapper.toAdminUserResponse(admin))
                    .thenReturn(AdminUserResponse.builder().roles(Set.of("ADMIN")).build());

            AdminUserResponse response = adminUserService.changeRole(ADMIN_ID, RoleName.ADMIN, ADMIN_ID);

            assertThat(response.getRoles()).containsExactly("ADMIN");
        }

        @Test
        void shouldThrowIllegalState_whenTargetRoleMissingFromDatabase() {
            User user = User.builder().email("awa@campuslink.io").password("hashed").build();
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminUserService.changeRole(TARGET_USER_ID, RoleName.TEACHER, ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===================== getStatistics =====================

    @Test
    void getStatistics_shouldAggregateCountsAcrossRepository() {
        when(userRepository.count()).thenReturn(42L);
        when(userRepository.countByStatus(any(AccountStatus.class))).thenReturn(5L);
        when(userRepository.countByRoles_Name(any(RoleName.class))).thenReturn(10L);
        when(userRepository.countByEmailVerifiedTrue()).thenReturn(30L);
        when(userRepository.countByEmailVerifiedFalse()).thenReturn(12L);
        when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(7L);

        AdminStatsResponse stats = adminUserService.getStatistics();

        assertThat(stats.getTotalUsers()).isEqualTo(42L);
        assertThat(stats.getUsersByStatus()).hasSize(AccountStatus.values().length);
        assertThat(stats.getUsersByRole()).hasSize(RoleName.values().length);
        assertThat(stats.getEmailVerifiedCount()).isEqualTo(30L);
        assertThat(stats.getEmailUnverifiedCount()).isEqualTo(12L);
        assertThat(stats.getNewUsersLast7Days()).isEqualTo(7L);
        assertThat(stats.getNewUsersLast30Days()).isEqualTo(7L);
    }

}
