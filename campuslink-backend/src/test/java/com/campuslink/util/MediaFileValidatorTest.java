package com.campuslink.util;

import com.campuslink.config.MediaProperties;
import com.campuslink.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaFileValidatorTest {

    private MediaProperties properties() {
        MediaProperties properties = new MediaProperties();
        properties.setMaxFileSizeBytes(5_242_880); // 5 Mo
        properties.setAllowedContentTypes(List.of("image/jpeg", "image/png", "image/webp"));
        return properties;
    }

    @Test
    void shouldPass_whenFileValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "contenu-image".getBytes());

        assertThatCode(() -> MediaFileValidator.validate(file, properties()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrow_whenFileIsNull() {
        assertThatThrownBy(() -> MediaFileValidator.validate(null, properties()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vide ou manquant");
    }

    @Test
    void shouldThrow_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> MediaFileValidator.validate(emptyFile, properties()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vide ou manquant");
    }

    @Test
    void shouldThrow_whenFileTooLarge() {
        byte[] tooLarge = new byte[6_000_000]; // > 5 Mo
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", tooLarge);

        assertThatThrownBy(() -> MediaFileValidator.validate(file, properties()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("taille maximale");
    }

    @Test
    void shouldThrow_whenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "contenu".getBytes());

        assertThatThrownBy(() -> MediaFileValidator.validate(file, properties()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Type de fichier non autorisé");
    }

    @Test
    void shouldThrow_whenContentTypeMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "photo", null, "contenu".getBytes());

        assertThatThrownBy(() -> MediaFileValidator.validate(file, properties()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Type de fichier non autorisé");
    }

}
