package com.campuslink.realtime.service;

import com.campuslink.config.ChatMediaProperties;
import com.campuslink.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMediaFileValidatorTest {
    private final ChatMediaProperties properties = new ChatMediaProperties();

    @Test
    void accepteUnJpegAvecSignatureCorrespondante() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        var validated = ChatMediaFileValidator.validate(file, properties);

        assertThat(validated.fileName()).isEqualTo("photo.jpg");
        assertThat(validated.contentType()).isEqualTo("image/jpeg");
        assertThat(validated.extension()).isEqualTo(".jpg");
    }

    @Test
    void rejetteExtensionEtMimeIncompatibles() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> ChatMediaFileValidator.validate(file, properties))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejetteContenuDontLaSignatureNeCorrespondPasAuMime() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "not an image".getBytes());

        assertThatThrownBy(() -> ChatMediaFileValidator.validate(file, properties))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejetteFichierAuDessusDeLaLimite() {
        properties.setMaxFileSizeBytes(3);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0});

        assertThatThrownBy(() -> ChatMediaFileValidator.validate(file, properties))
                .isInstanceOf(BadRequestException.class);
    }
}
