package com.campuslink.realtime.service;

import com.campuslink.config.CloudinaryProperties;
import com.campuslink.exception.MediaUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatMediaStorageTest {
    private Cloudinary cloudinary;
    private CloudinaryProperties properties;
    private Uploader uploader;
    private ChatMediaStorage storage;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        properties = new CloudinaryProperties();
        properties.setCloudName("test-cloud");
        properties.setApiKey("test-key");
        properties.setApiSecret("test-secret");
        when(cloudinary.uploader()).thenReturn(uploader);
        storage = new ChatMediaStorage(cloudinary, properties);
    }

    @Test
    void stockeLesImagesDansUnDossierCloudinaryAuthentifie() throws Exception {
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            Map<?, ?> options = invocation.getArgument(1);
            return Map.of("public_id", options.get("public_id"));
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String key = storage.store(file, ".jpg");

        assertThat(key).startsWith("image:campuslink-chat/");
        assertThat(key).doesNotEndWith(".jpg");
        verify(uploader).upload(eq(new byte[]{1, 2, 3}), argThat(options ->
                "image".equals(options.get("resource_type"))
                        && "authenticated".equals(options.get("type"))
                        && options.get("public_id").toString().startsWith("campuslink-chat/")
                        && Boolean.FALSE.equals(options.get("overwrite"))));
    }

    @Test
    void stockeLesDocumentsRawAvecLeurExtensionDansUnDossierAuthentifie() throws Exception {
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            Map<?, ?> options = invocation.getArgument(1);
            return Map.of("public_id", options.get("public_id"));
        });

        String key = storage.store(new MockMultipartFile(
                "file", "rapport.pdf", "application/pdf", new byte[]{1, 2, 3}), ".pdf");

        assertThat(key).startsWith("raw:campuslink-chat/");
        assertThat(key).endsWith(".pdf");
        verify(uploader).upload(any(byte[].class), argThat(options ->
                "raw".equals(options.get("resource_type"))
                        && "authenticated".equals(options.get("type"))
                        && options.get("public_id").toString().endsWith(".pdf")));
    }

    @Test
    void supprimeUneRessourceAuthentifieeAvecLeBonTypeCloudinary() throws Exception {
        when(uploader.destroy(eq("campuslink-chat/7ce2e4a3-e579-49b0-84bb-cd41b04c4e43"),
                anyMap())).thenReturn(Map.of("result", "ok"));

        storage.delete("image:campuslink-chat/7ce2e4a3-e579-49b0-84bb-cd41b04c4e43");

        verify(uploader).destroy(eq("campuslink-chat/7ce2e4a3-e579-49b0-84bb-cd41b04c4e43"),
                argThat(options -> "image".equals(options.get("resource_type"))
                        && "authenticated".equals(options.get("type"))));
    }

    @Test
    void refuseLeStockageQuandCloudinaryNestPasConfigure() {
        properties.setApiSecret("");
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThrows(MediaUploadException.class, () -> storage.store(file, ".jpg"));
        verifyNoInteractions(uploader);
    }

    @Test
    void ignoreLesClesInvalidesSansAccederAuFournisseur() {
        assertThat(storage.load("../autre-fichier")).isEmpty();
        storage.delete("raw:autre-dossier/document.pdf");
        verifyNoInteractions(cloudinary, uploader);
    }
}
