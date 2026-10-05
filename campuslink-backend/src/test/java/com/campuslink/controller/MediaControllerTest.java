package com.campuslink.controller;

import com.campuslink.dto.response.MediaResponse;
import com.campuslink.exception.BadRequestException;
import com.campuslink.exception.MediaUploadException;
import com.campuslink.security.JwtService;
import com.campuslink.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du module média, isolés via {@link WebMvcTest}
 * (voir {@link AuthControllerTest} pour le raisonnement sur {@code addFilters = false}).
 */
@WebMvcTest(controllers = MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    /** Voir {@link AuthControllerTest} : nécessaire pour la slice {@code @WebMvcTest}. */
    @MockBean
    private JwtService jwtService;

    // ===================== POST /media/upload =====================

    @Test
    void upload_shouldReturn201_whenFileValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        MediaResponse response = MediaResponse.builder()
                .url("https://res.cloudinary.com/campuslink/photo.jpg")
                .publicId("campuslink/photo-abc123")
                .format("jpg")
                .width(1080)
                .height(720)
                .bytes(204800L)
                .build();

        when(mediaService.upload(any())).thenReturn(response);

        mockMvc.perform(multipart("/media/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/campuslink/photo.jpg"))
                .andExpect(jsonPath("$.data.publicId").value("campuslink/photo-abc123"));
    }

    @Test
    void upload_shouldReturn400_whenFileInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "contenu".getBytes());

        when(mediaService.upload(any()))
                .thenThrow(new BadRequestException("Type de fichier non autorisé. Formats acceptés : image/jpeg, image/png"));

        mockMvc.perform(multipart("/media/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_shouldReturn502_whenCloudinaryFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        when(mediaService.upload(any()))
                .thenThrow(new MediaUploadException("Impossible d'uploader le fichier pour le moment."));

        mockMvc.perform(multipart("/media/upload").file(file))
                .andExpect(status().isBadGateway());
    }

    // ===================== PUT /media/replace =====================

    @Test
    void replace_shouldReturn200_whenValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        MediaResponse response = MediaResponse.builder()
                .url("https://res.cloudinary.com/campuslink/new-photo.jpg")
                .publicId("campuslink/new-photo-id")
                .build();

        when(mediaService.replace(eq("campuslink/old-photo-id"), any())).thenReturn(response);

        mockMvc.perform(multipart("/media/replace")
                        .file(file)
                        .param("publicId", "campuslink/old-photo-id")
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicId").value("campuslink/new-photo-id"));

        verify(mediaService).replace(eq("campuslink/old-photo-id"), any());
    }

    @Test
    void replace_shouldReturn400_whenPublicIdBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        mockMvc.perform(multipart("/media/replace")
                        .file(file)
                        .param("publicId", "")
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isBadRequest());
    }

    // ===================== DELETE /media =====================

    @Test
    void delete_shouldReturn200_whenValid() throws Exception {
        mockMvc.perform(delete("/media").param("publicId", "campuslink/photo-abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(mediaService).delete("campuslink/photo-abc123");
    }

    @Test
    void delete_shouldReturn400_whenPublicIdMissing() throws Exception {
        mockMvc.perform(delete("/media"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldReturn502_whenCloudinaryFails() throws Exception {
        doThrow(new MediaUploadException("Impossible de supprimer le fichier pour le moment."))
                .when(mediaService).delete("campuslink/photo-abc123");

        mockMvc.perform(delete("/media").param("publicId", "campuslink/photo-abc123"))
                .andExpect(status().isBadGateway());
    }

}
