package com.campuslink.controller;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.User;
import com.campuslink.exception.DuplicateResourceException;
import com.campuslink.exception.ResourceNotFoundException;
import com.campuslink.security.JwtService;
import com.campuslink.security.UserPrincipal;
import com.campuslink.service.ProfileSearchService;
import com.campuslink.service.ProfileService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du module Profile, isolés via {@link WebMvcTest}
 * (voir {@link AuthControllerTest} pour le raisonnement sur {@code addFilters = false}).
 *
 * <p>Les routes {@code /me} dépendent de {@code @AuthenticationPrincipal} :
 * comme les filtres de sécurité sont désactivés, le principal est injecté
 * manuellement dans le {@link org.springframework.security.core.context.SecurityContext}
 * via {@link SecurityMockMvcRequestPostProcessors#authentication}.</p>
 */
@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private ProfileSearchService profileSearchService;

    /**
     * Fourni uniquement pour satisfaire la dépendance de {@code JwtAuthenticationFilter},
     * inclus automatiquement par la tranche {@code @WebMvcTest} (les beans {@code Filter}
     * font partie de la slice) alors que les filtres ne s'exécutent pas ici
     * ({@code addFilters = false}).
     */
    @MockBean
    private JwtService jwtService;

    /**
     * Construit le post-processor qui authentifie la requête MockMvc.
     *
     * <p>La tranche tourne avec {@code addFilters = false} : la
     * {@code SecurityContextHolderFilter} ne s'exécute donc jamais et le
     * SecurityContext que spring-security-test écrit dans la session n'est
     * jamais relu — {@code @AuthenticationPrincipal} resterait {@code null}
     * (NPE dans le controller -> 500). On peuple donc aussi le
     * {@link SecurityContextHolder} directement : MockMvc exécute la requête
     * sur le même thread que le test.</p>
     */
    private RequestPostProcessor authenticated() {
        User user = User.builder().email("etudiant@campuslink.io").password("hashed").build();
        user.setId(USER_ID);
        UserPrincipal principal = new UserPrincipal(user);
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

    // ===================== POST /profiles/me =====================

    @Test
    void createProfile_shouldReturn201_whenValid() throws Exception {
        ProfileRequest request = ProfileRequest.builder()
                .firstName("Awa")
                .lastName("Kone")
                .city("Bouaké")
                .interests(Set.of("Musique"))
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(UUID.randomUUID())
                .firstName("Awa")
                .lastName("Kone")
                .build();

        when(profileService.createProfile(eq(USER_ID), any(ProfileRequest.class))).thenReturn(response);

        mockMvc.perform(post("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("Awa"));
    }

    @Test
    void createProfile_shouldReturn409_whenProfileAlreadyExists() throws Exception {
        ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

        when(profileService.createProfile(eq(USER_ID), any(ProfileRequest.class)))
                .thenThrow(new DuplicateResourceException("Un profil existe déjà pour cet utilisateur."));

        mockMvc.perform(post("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createProfile_shouldReturn400_whenFirstNameBlank() throws Exception {
        ProfileRequest request = ProfileRequest.builder().firstName("").lastName("Kone").build();

        mockMvc.perform(post("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===================== PUT /profiles/me =====================

    @Test
    void updateProfile_shouldReturn200_whenValid() throws Exception {
        ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

        ProfileResponse response = ProfileResponse.builder().firstName("Awa").lastName("Kone").build();
        when(profileService.updateProfile(eq(USER_ID), any(ProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Awa"));
    }

    @Test
    void updateProfile_shouldReturn404_whenNoProfileExists() throws Exception {
        ProfileRequest request = ProfileRequest.builder().firstName("Awa").lastName("Kone").build();

        when(profileService.updateProfile(eq(USER_ID), any(ProfileRequest.class)))
                .thenThrow(new ResourceNotFoundException("Profil", "utilisateur", USER_ID));

        mockMvc.perform(put("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_shouldReturn400_whenTooManyInterests() throws Exception {
        Set<String> tooMany = new java.util.HashSet<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add("interet-" + i);
        }
        ProfileRequest request = ProfileRequest.builder()
                .firstName("Awa")
                .lastName("Kone")
                .interests(tooMany)
                .build();

        mockMvc.perform(put("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_shouldReturn400_whenBodyMalformed() throws Exception {
        // JSON tronqué : l'erreur est côté client, elle ne doit pas remonter en 500.
        mockMvc.perform(put("/profiles/me")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Awa\""))
                .andExpect(status().isBadRequest());
    }

    // ===================== DELETE /profiles/me =====================

    @Test
    void deleteProfile_shouldReturn200_whenProfileExists() throws Exception {
        mockMvc.perform(delete("/profiles/me").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteProfile_shouldReturn404_whenNoProfileExists() throws Exception {
        doThrow(new ResourceNotFoundException("Profil", "utilisateur", USER_ID))
                .when(profileService).deleteProfile(USER_ID);

        mockMvc.perform(delete("/profiles/me").with(authenticated()))
                .andExpect(status().isNotFound());
    }

    // ===================== GET /profiles/me =====================

    @Test
    void getOwnProfile_shouldReturn200() throws Exception {
        ProfileResponse response = ProfileResponse.builder().firstName("Awa").lastName("Kone").build();
        when(profileService.getOwnProfile(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/profiles/me").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Awa"));
    }

    // ===================== GET /profiles/{id}/public =====================

    @Test
    void getPublicProfile_shouldReturn200_withoutAuthentication() throws Exception {
        UUID profileId = UUID.randomUUID();
        ProfileResponse response = ProfileResponse.builder()
                .id(profileId)
                .firstName("Moussa")
                .lastName("Traoré")
                .age(22)
                .interests(Set.of("Football"))
                .build();

        when(profileService.getPublicProfile(profileId)).thenReturn(response);

        // Volontairement aucun .with(authenticated()) : la route est publique.
        mockMvc.perform(get("/profiles/{profileId}/public", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Moussa"))
                .andExpect(jsonPath("$.data.age").value(22));
    }

    @Test
    void getPublicProfile_shouldReturn404_whenProfileUnknown() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(profileService.getPublicProfile(profileId))
                .thenThrow(new ResourceNotFoundException("Profil", "id", profileId));

        mockMvc.perform(get("/profiles/{profileId}/public", profileId))
                .andExpect(status().isNotFound());
    }

    // ===================== GET /profiles/search =====================

    @Test
    void search_shouldReturn200_withPaginatedResults_withoutAuthentication() throws Exception {
        ProfileResponse profile = ProfileResponse.builder()
                .firstName("Awa")
                .city("Bouaké")
                .age(21)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<ProfileResponse> page = new PageImpl<>(List.of(profile), pageable, 1);

        when(profileSearchService.search(any(), any())).thenReturn(page);

        // Volontairement aucun .with(authenticated()) : la recherche est publique.
        mockMvc.perform(get("/profiles/search")
                        .param("minAge", "18")
                        .param("maxAge", "25")
                        .param("city", "Bouaké"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].firstName").value("Awa"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.first").value(true));
    }

    @Test
    void search_shouldReturn200_whenNoFiltersProvided() throws Exception {
        Page<ProfileResponse> emptyPage = new PageImpl<>(List.of());
        when(profileSearchService.search(any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/profiles/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void search_shouldReturn400_whenMinAgeNegative() throws Exception {
        mockMvc.perform(get("/profiles/search").param("minAge", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldAcceptMultipleInterests_asRepeatedParams() throws Exception {
        Page<ProfileResponse> emptyPage = new PageImpl<>(List.of());
        when(profileSearchService.search(any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/profiles/search")
                        .param("interests", "Football")
                        .param("interests", "Musique"))
                .andExpect(status().isOk());

        verify(profileSearchService).search(argThat(criteria ->
                criteria.getInterests() != null
                        && criteria.getInterests().contains("Football")
                        && criteria.getInterests().contains("Musique")
        ), any());
    }

    @Test
    void search_shouldPassSortParameter_toService() throws Exception {
        Page<ProfileResponse> emptyPage = new PageImpl<>(List.of());
        when(profileSearchService.search(any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/profiles/search").param("sort", "age,desc"))
                .andExpect(status().isOk());

        verify(profileSearchService).search(any(), argThat(pageable ->
                pageable.getSort().getOrderFor("age") != null
                        && pageable.getSort().getOrderFor("age").getDirection().isDescending()
        ));
    }

}
