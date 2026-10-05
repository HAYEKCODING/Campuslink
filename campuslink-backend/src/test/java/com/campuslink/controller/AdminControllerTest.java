package com.campuslink.controller;

import com.campuslink.dto.request.AdminUpdateUserRequest;
import com.campuslink.dto.request.ChangeRoleRequest;
import com.campuslink.dto.response.AdminStatsResponse;
import com.campuslink.dto.response.AdminUserResponse;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.exception.BadRequestException;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du module Administration (contrat HTTP uniquement —
 * voir {@code AdminSecurityIntegrationTest} pour la vérification réelle de ROLE_ADMIN).
 */
@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserService adminUserService;

    /** Voir {@link AuthControllerTest} : nécessaire pour la slice {@code @WebMvcTest}. */
    @MockBean
    private JwtService jwtService;

    /**
     * Construit le post-processor qui authentifie la requête comme administrateur.
     *
     * <p>Voir {@link ProfileControllerTest} : avec {@code addFilters = false}, le
     * SecurityContext de spring-security-test n'est jamais relu, on le place donc
     * directement dans le {@link SecurityContextHolder}.</p>
     */
    private RequestPostProcessor authenticatedAsAdmin() {
        User admin = User.builder().email("admin@campuslink.io").password("hashed").build();
        admin.setId(ADMIN_ID);
        UserPrincipal principal = new UserPrincipal(admin);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(token);
            return request;
        };
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsers_shouldReturn200_withPaginatedResults() throws Exception {
        AdminUserResponse user = AdminUserResponse.builder().email("awa@campuslink.io").build();
        Page<AdminUserResponse> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(adminUserService.listUsers(any(), any())).thenReturn(page);

        mockMvc.perform(get("/admin/users").with(authenticatedAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("awa@campuslink.io"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listUsers_shouldPassFilters_toService() throws Exception {
        when(adminUserService.listUsers(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/users").with(authenticatedAsAdmin())
                        .param("status", "SUSPENDED").param("role", "ADMIN"))
                .andExpect(status().isOk());

        verify(adminUserService).listUsers(org.mockito.ArgumentMatchers.argThat(c ->
                c.getStatus() == AccountStatus.SUSPENDED && c.getRole() == RoleName.ADMIN), any());
    }

    @Test
    void updateUser_shouldReturn200_whenValid() throws Exception {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().email("nouveau@campuslink.io").build();
        when(adminUserService.updateUser(eq(TARGET_USER_ID), any()))
                .thenReturn(AdminUserResponse.builder().email("nouveau@campuslink.io").build());

        mockMvc.perform(patch("/admin/users/{userId}", TARGET_USER_ID).with(authenticatedAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("nouveau@campuslink.io"));
    }

    @Test
    void updateUser_shouldReturn409_whenEmailAlreadyTaken() throws Exception {
        when(adminUserService.updateUser(eq(TARGET_USER_ID), any()))
                .thenThrow(new DuplicateResourceException("Un compte existe déjà avec l'email : pris@campuslink.io"));

        mockMvc.perform(patch("/admin/users/{userId}", TARGET_USER_ID).with(authenticatedAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AdminUpdateUserRequest.builder().email("pris@campuslink.io").build())))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_shouldReturn404_whenUserUnknown() throws Exception {
        when(adminUserService.updateUser(eq(TARGET_USER_ID), any()))
                .thenThrow(new ResourceNotFoundException("Utilisateur", "id", TARGET_USER_ID));

        mockMvc.perform(patch("/admin/users/{userId}", TARGET_USER_ID).with(authenticatedAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AdminUpdateUserRequest.builder().build())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldReturn200_andPassActingAdminId() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", TARGET_USER_ID).with(authenticatedAsAdmin()))
                .andExpect(status().isOk());
        verify(adminUserService).deleteUser(TARGET_USER_ID, ADMIN_ID);
    }

    @Test
    void deleteUser_shouldReturn400_whenTargetingSelf() throws Exception {
        doThrow(new BadRequestException("Vous ne pouvez pas supprimer votre propre compte via ce module."))
                .when(adminUserService).deleteUser(ADMIN_ID, ADMIN_ID);

        mockMvc.perform(delete("/admin/users/{userId}", ADMIN_ID).with(authenticatedAsAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suspendUser_shouldReturn200() throws Exception {
        when(adminUserService.suspendUser(TARGET_USER_ID, ADMIN_ID))
                .thenReturn(AdminUserResponse.builder().status(AccountStatus.SUSPENDED).build());

        mockMvc.perform(patch("/admin/users/{userId}/suspend", TARGET_USER_ID).with(authenticatedAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void activateUser_shouldReturn200() throws Exception {
        when(adminUserService.activateUser(TARGET_USER_ID))
                .thenReturn(AdminUserResponse.builder().status(AccountStatus.ACTIVE).build());

        mockMvc.perform(patch("/admin/users/{userId}/activate", TARGET_USER_ID).with(authenticatedAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void changeRole_shouldReturn200_whenValid() throws Exception {
        ChangeRoleRequest request = ChangeRoleRequest.builder().role(RoleName.TEACHER).build();
        when(adminUserService.changeRole(TARGET_USER_ID, RoleName.TEACHER, ADMIN_ID))
                .thenReturn(AdminUserResponse.builder().roles(Set.of("TEACHER")).build());

        mockMvc.perform(patch("/admin/users/{userId}/role", TARGET_USER_ID).with(authenticatedAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("TEACHER"));
    }

    @Test
    void changeRole_shouldReturn400_whenRoleMissing() throws Exception {
        mockMvc.perform(patch("/admin/users/{userId}/role", TARGET_USER_ID).with(authenticatedAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatistics_shouldReturn200() throws Exception {
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(100L)
                .usersByStatus(Map.of("ACTIVE", 80L))
                .usersByRole(Map.of("STUDENT", 90L))
                .emailVerifiedCount(70L).emailUnverifiedCount(30L)
                .newUsersLast7Days(4L).newUsersLast30Days(15L)
                .build();
        when(adminUserService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/admin/stats").with(authenticatedAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.usersByStatus.ACTIVE").value(80));
    }

}
