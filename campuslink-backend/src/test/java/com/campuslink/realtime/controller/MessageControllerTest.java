package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.request.MessageRequest;
import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MessageService;
import com.campuslink.security.JwtService;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Tests de la couche web du module Messagerie, isolés via {@link WebMvcTest}
 * (voir {@link com.campuslink.controller.AuthControllerTest} pour le
 * raisonnement sur {@code addFilters = false} et sur le mock {@code JwtService}).
 *
 * <p>L'utilisateur courant n'est pas résolu par JWT ici : {@code CurrentUserPort}
 * est mocké et renvoie directement l'identifiant attendu.</p>
 */
@WebMvcTest(controllers = MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private CurrentUserPort currentUserPort;

    /** Voir {@link com.campuslink.controller.AuthControllerTest} : nécessaire pour la slice {@code @WebMvcTest}. */
    @MockBean
    private JwtService jwtService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===================== POST /matches/{matchId}/messages =====================

    @Test
    void envoyer_shouldReturn200_whenPayloadValid() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(2L);

        MessageResponse response = MessageResponse.builder()
                .id(500L)
                .matchId(10L)
                .expediteurId(2L)
                .contenu("Salut !")
                .build();
        when(messageService.envoyer(eq(2L), eq(10L), eq("Salut !"))).thenReturn(response);

        MessageRequest request = new MessageRequest(10L, "Salut !");

        mockMvc.perform(post("/matches/{matchId}/messages", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"contenu\":\"Salut !\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.contenu").value("Salut !"));

        verify(messageService).envoyer(eq(2L), eq(10L), eq(request.getContenu()));
    }

    @Test
    void envoyer_shouldReturn400_whenContenuBlank() throws Exception {
        mockMvc.perform(post("/matches/{matchId}/messages", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":10,\"contenu\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void envoyer_shouldReturn400_whenMatchIdMissingFromBody() throws Exception {
        mockMvc.perform(post("/matches/{matchId}/messages", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenu\":\"Salut\"}"))
                .andExpect(status().isBadRequest());
    }

    // ===================== GET /matches/{matchId}/messages =====================

    @Test
    void historique_shouldReturn200_withPaginatedResults() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(2L);

        MessageResponse message = MessageResponse.builder()
                .id(1L)
                .matchId(10L)
                .expediteurId(1L)
                .contenu("Coucou")
                .build();
        when(messageService.getHistorique(eq(2L), eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(message), PageRequest.of(0, 30), 1));

        mockMvc.perform(get("/matches/{matchId}/messages", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contenu").value("Coucou"));
    }

    // ===================== POST /matches/{matchId}/messages/lu =====================

    @Test
    void marquerCommeLu_shouldReturn204() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(2L);

        mockMvc.perform(post("/matches/{matchId}/messages/lu", 10L))
                .andExpect(status().isNoContent());

        verify(messageService).marquerCommeLu(2L, 10L);
    }
}
