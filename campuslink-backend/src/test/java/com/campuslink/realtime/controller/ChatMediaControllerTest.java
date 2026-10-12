package com.campuslink.realtime.controller;

import com.campuslink.realtime.dto.response.ChatMediaDownload;
import com.campuslink.realtime.dto.response.ChatMediaLibraryItemResponse;
import com.campuslink.realtime.dto.response.ChatMediaResponse;
import com.campuslink.realtime.dto.response.MessageResponse;
import com.campuslink.realtime.integration.CurrentUserPort;
import com.campuslink.realtime.service.MessageService;
import com.campuslink.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatMediaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;
    @MockBean
    private CurrentUserPort currentUserPort;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void envoyer_transmetLeFichierEtLeMatchAuService() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(7L);
        when(messageService.envoyerMedia(eq(7L), eq(13L), any())).thenReturn(MessageResponse.builder()
                .id(22L)
                .matchId(13L)
                .expediteurId(7L)
                .media(ChatMediaResponse.builder()
                        .fileName("photo.jpg")
                        .contentType("image/jpeg")
                        .sizeBytes(4L)
                        .build())
                .build());

        mockMvc.perform(multipart("/matches/13/media")
                        .file("file", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media.fileName").value("photo.jpg"));

        verify(messageService).envoyerMedia(eq(7L), eq(13L), any());
    }

    @Test
    void bibliotheque_retourneLaPageDeLaCategorieDemandee() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(7L);
        when(messageService.getMediaLibrary(eq(7L), eq(13L), eq(com.campuslink.realtime.entity.enums.ChatMediaCategory.LINKS), any()))
                .thenReturn(new PageImpl<>(List.of(ChatMediaLibraryItemResponse.builder()
                        .messageId(22L)
                        .senderId(7L)
                        .category("LINKS")
                        .links(List.of("https://example.org"))
                        .build()), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/matches/13/media")
                        .param("category", "LINKS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].links[0]").value("https://example.org"));
    }

    @Test
    void contenu_sertLeFichierSansLeRendrePublicementMiseEnCache() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(7L);
        byte[] bytes = {1, 2, 3, 4};
        when(messageService.getMedia(7L, 13L, 22L)).thenReturn(new ChatMediaDownload(
                new ByteArrayResource(bytes), "document.pdf", "application/pdf", bytes.length));

        mockMvc.perform(get("/matches/13/media/22/content"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
