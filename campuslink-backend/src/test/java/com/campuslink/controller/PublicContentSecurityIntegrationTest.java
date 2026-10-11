package com.campuslink.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preuve de bout en bout que les routes de la landing sont réellement
 * accessibles <strong>sans authentification</strong> (chaîne de filtres
 * Spring Security active, contrairement à PublicContentControllerTest qui
 * la désactive).
 *
 * <p>C'est ce test qui garantit le correctif du bug « visiteur anonyme
 * redirigé vers /connexion » : si une de ces routes retournait un 401,
 * {@code lib/api.js} émettrait {@code campuslink:unauthorized} et déconnecterait.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicContentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statsPublic_shouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/stats/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberCount").isNumber());
    }

    @Test
    void testimonials_shouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void referenceUniversities_shouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/reference/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void referenceUnknownType_shouldReturn400_not401() throws Exception {
        // 400 et non 401 : l'anonyme ne doit jamais être traité comme une
        // session expirée, même sur une erreur de paramètre.
        mockMvc.perform(get("/reference/inconnu"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contact_shouldAcceptValidMessageAnonymously() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Visiteur\",\"email\":\"v@exemple.com\",\"message\":\"Bonjour\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void contact_shouldReturn400_not401_whenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"pas-un-email\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profilesSearch_shouldAcceptGenderFilterAnonymously() throws Exception {
        mockMvc.perform(get("/profiles/search").param("gender", "FEMALE"))
                .andExpect(status().isOk());
    }

    @Test
    void profilesSearch_shouldReturn400_forUnknownGenderValue() throws Exception {
        mockMvc.perform(get("/profiles/search").param("gender", "AUTRE_CHOSE"))
                .andExpect(status().isBadRequest());
    }

}
