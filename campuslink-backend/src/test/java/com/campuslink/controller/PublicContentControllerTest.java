package com.campuslink.controller;

import com.campuslink.dto.request.ContactRequest;
import com.campuslink.dto.response.PublicStatsResponse;
import com.campuslink.dto.response.TestimonialResponse;
import com.campuslink.exception.BadRequestException;
import com.campuslink.security.JwtService;
import com.campuslink.service.PublicContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web des endpoints publics de la landing
 * ({@code /stats/public}, {@code /testimonials}, {@code /contact},
 * {@code /reference/{type}}), isolés via {@link WebMvcTest}.
 *
 * <p>Comme pour {@code ProfileControllerTest}, les filtres de sécurité
 * sont désactivés ({@code addFilters = false}) : ces routes sont de toute
 * façon publiques (elles figurent dans
 * {@code SecurityConstants.PUBLIC_ENDPOINTS}), le test de leur accessibilité
 * anonyme vivant dans {@code AdminSecurityIntegrationTest}.</p>
 */
@WebMvcTest(controllers = PublicContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublicContentService publicContentService;

    /**
     * Fourni uniquement pour satisfaire la dépendance de
     * {@code JwtAuthenticationFilter} (bean {@code Filter} inclus dans la
     * tranche {@code @WebMvcTest} mais inactif ici).
     */
    @MockBean
    private JwtService jwtService;

    // ===================== GET /stats/public =====================

    @Test
    void getPublicStats_shouldReturn200_withEnvelope() throws Exception {
        when(publicContentService.getPublicStats()).thenReturn(
                PublicStatsResponse.builder()
                        .memberCount(128L)
                        .avatarPhotos(List.of("https://cdn/a.jpg"))
                        .build());

        mockMvc.perform(get("/stats/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberCount").value(128))
                .andExpect(jsonPath("$.data.avatarPhotos[0]").value("https://cdn/a.jpg"));
    }

    // ===================== GET /testimonials =====================

    @Test
    void getTestimonials_shouldReturn200_withList() throws Exception {
        when(publicContentService.getTestimonials()).thenReturn(List.of(
                TestimonialResponse.builder()
                        .id(java.util.UUID.randomUUID())
                        .name("Awa")
                        .role("Étudiante en médecine")
                        .quote("Très utile.")
                        .build()));

        mockMvc.perform(get("/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Awa"))
                .andExpect(jsonPath("$.data[0].quote").value("Très utile."));
    }

    @Test
    void getTestimonials_shouldReturn200_emptyList_whenNone() throws Exception {
        when(publicContentService.getTestimonials()).thenReturn(List.of());

        mockMvc.perform(get("/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ===================== POST /contact =====================

    @Test
    void submitContact_shouldReturn201_whenValid() throws Exception {
        ContactRequest request = ContactRequest.builder()
                .name("Koffi")
                .email("koffi@example.com")
                .message("Bonjour, une question.")
                .build();

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message envoyé, nous vous répondrons au plus vite."));

        verify(publicContentService).submitContact(any(ContactRequest.class));
    }

    @Test
    void submitContact_shouldReturn400_whenEmailInvalid() throws Exception {
        ContactRequest request = ContactRequest.builder()
                .name("Koffi")
                .email("pas-un-email")
                .message("Bonjour.")
                .build();

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitContact_shouldReturn400_whenMessageBlank() throws Exception {
        ContactRequest request = ContactRequest.builder()
                .name("Koffi")
                .email("koffi@example.com")
                .message("   ")
                .build();

        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===================== GET /reference/{type} =====================

    @Test
    void getReferenceValues_shouldReturn200_whenTypeKnown() throws Exception {
        when(publicContentService.findReferenceValues("universities"))
                .thenReturn(List.of("INP-HB", "UFHB"));

        mockMvc.perform(get("/reference/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("INP-HB"))
                .andExpect(jsonPath("$.data[1]").value("UFHB"));
    }

    @Test
    void getReferenceValues_shouldReturn400_whenTypeUnknown() throws Exception {
        when(publicContentService.findReferenceValues("couleurs"))
                .thenThrow(new BadRequestException("Référentiel inconnu : 'couleurs'."));

        mockMvc.perform(get("/reference/couleurs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Référentiel inconnu : 'couleurs'."));
    }

    @Test
    void getReferenceValues_shouldDelegateThePathVariable() throws Exception {
        when(publicContentService.findReferenceValues("interests")).thenReturn(List.of());

        mockMvc.perform(get("/reference/interests"))
                .andExpect(status().isOk());

        verify(publicContentService).findReferenceValues(eq("interests"));
    }

}
