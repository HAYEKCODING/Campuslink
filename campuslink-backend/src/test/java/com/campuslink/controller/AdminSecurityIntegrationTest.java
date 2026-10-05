package com.campuslink.controller;

import com.campuslink.dto.response.AdminStatsResponse;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.RoleName;
import com.campuslink.repository.RoleRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie de bout en bout que {@code /admin/**} est réellement protégé par
 * ROLE_ADMIN — chaîne de filtres ET @PreAuthorize réellement actifs
 * (contrairement à AdminControllerTest, qui les désactive pour se concentrer
 * sur la logique du controller). C'est ce test qui prouve l'exigence
 * "tous les endpoints doivent être protégés avec ROLE_ADMIN".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private AdminUserService adminUserService;

    private Role getOrCreateRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(new UserPrincipal(user));
    }

    private User persistUserWithRole(String email, RoleName roleName) {
        User user = User.builder().email(email).password(passwordEncoder.encode("Secret123")).build();
        user.addRole(getOrCreateRole(roleName));
        return userRepository.save(user);
    }

    @Test
    void adminEndpoint_shouldReturn200_forUserWithAdminRole() throws Exception {
        User admin = persistUserWithRole("admin-security-test@campuslink.io", RoleName.ADMIN);
        when(adminUserService.getStatistics()).thenReturn(AdminStatsResponse.builder().totalUsers(0).build());

        mockMvc.perform(get("/admin/stats").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_shouldReturn403_forAuthenticatedNonAdminUser() throws Exception {
        User student = persistUserWithRole("student-security-test@campuslink.io", RoleName.STUDENT);

        mockMvc.perform(get("/admin/stats").header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_shouldReturn403_forUserWithNoRoleAtAll() throws Exception {
        User noRoleUser = User.builder().email("norole-security-test@campuslink.io")
                .password(passwordEncoder.encode("Secret123")).build();
        userRepository.save(noRoleUser);

        mockMvc.perform(get("/admin/stats").header("Authorization", "Bearer " + tokenFor(noRoleUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_shouldReturn401_forUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_shouldReturn401_forInvalidToken() throws Exception {
        mockMvc.perform(get("/admin/stats").header("Authorization", "Bearer token-invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminUsersListEndpoint_shouldAlsoBeProtected() throws Exception {
        User student = persistUserWithRole("student-security-test-2@campuslink.io", RoleName.STUDENT);

        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminEndpoint_shouldRemainUnaffected_byAdminRoleRestriction() throws Exception {
        User student = persistUserWithRole("student-security-test-3@campuslink.io", RoleName.STUDENT);

        mockMvc.perform(get("/profiles/me").header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isNotFound());
    }

}
