package com.campuslink.service.impl;

import com.campuslink.config.CloudinaryProperties;
import com.campuslink.config.MediaProperties;
import com.campuslink.dto.response.MediaResponse;
import com.campuslink.exception.BadRequestException;
import com.campuslink.exception.MediaUploadException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link MediaServiceImpl}, isolés du réseau et de
 * Cloudinary grâce à Mockito (le SDK {@link Cloudinary}/{@link Uploader}
 * est entièrement mocké — aucun appel HTTP réel n'est effectué).
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private MediaProperties mediaProperties;
    private CloudinaryProperties cloudinaryProperties;
    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        mediaProperties = new MediaProperties();
        mediaProperties.setFolder("campuslink-test");
        mediaProperties.setMaxFileSizeBytes(5_242_880);
        mediaProperties.setAllowedContentTypes(List.of("image/jpeg", "image/png", "image/webp"));
        mediaProperties.setQuality("auto:good");
        mediaProperties.setMaxDimension(1080);

        // Credentials renseignés = mode Cloudinary (chemin par défaut des tests).
        cloudinaryProperties = new CloudinaryProperties();
        cloudinaryProperties.setCloudName("campuslink");
        cloudinaryProperties.setApiKey("api-key");
        cloudinaryProperties.setApiSecret("api-secret");

        mediaService = new MediaServiceImpl(cloudinary, cloudinaryProperties, mediaProperties);
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** Service en mode local (aucun credential Cloudinary) avec dossier dédié. */
    private MediaServiceImpl localMediaService(Path tempDir) {
        CloudinaryProperties empty = new CloudinaryProperties();
        MediaProperties props = new MediaProperties();
        props.setFolder("campuslink-test");
        props.setMaxFileSizeBytes(5_242_880);
        props.setAllowedContentTypes(List.of("image/jpeg", "image/png", "image/webp"));
        props.setQuality("auto:good");
        props.setMaxDimension(1080);
        props.setLocalDir(tempDir.toString());
        return new MediaServiceImpl(cloudinary, empty, props);
    }

    private void mockRequestContext() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/media/upload")));
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu-image".getBytes());
    }

    // ===================== upload =====================

    @Nested
    class Upload {

        @Test
        void shouldReturnMediaResponse_whenUploadSucceeds() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenReturn(Map.of(
                    "secure_url", "https://res.cloudinary.com/campuslink/image/upload/v1/campuslink-test/abc123.jpg",
                    "public_id", "campuslink-test/abc123",
                    "format", "jpg",
                    "width", 1080,
                    "height", 720,
                    "bytes", 204800
            ));

            MediaResponse response = mediaService.upload(validFile());

            assertThat(response.getUrl()).startsWith("https://res.cloudinary.com");
            assertThat(response.getPublicId()).isEqualTo("campuslink-test/abc123");
            assertThat(response.getFormat()).isEqualTo("jpg");
            assertThat(response.getWidth()).isEqualTo(1080);
            assertThat(response.getHeight()).isEqualTo(720);
            assertThat(response.getBytes()).isEqualTo(204800L);
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldSendCompressionOptions_toCloudinary() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenReturn(Map.of(
                    "secure_url", "https://res.cloudinary.com/x.jpg",
                    "public_id", "x"
            ));

            mediaService.upload(validFile());

            verify(uploader).upload(any(), org.mockito.ArgumentMatchers.argThat(options -> {
                if (!(options instanceof Map<?, ?> map)) {
                    return false;
                }
                Object transformation = map.get("transformation");
                return transformation instanceof Map<?, ?> transformationMap
                        && "auto:good".equals(transformationMap.get("quality"))
                        && "auto".equals(transformationMap.get("fetch_format"))
                        && "limit".equals(transformationMap.get("crop"));
            }));
        }

        @Test
        void shouldThrowBadRequest_whenFileInvalid_withoutCallingCloudinary() {
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "contenu".getBytes());

            assertThatThrownBy(() -> mediaService.upload(invalidFile))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void shouldThrowMediaUploadException_whenCloudinaryThrowsIOException() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenThrow(new IOException("Connexion refusée"));

            assertThatThrownBy(() -> mediaService.upload(validFile()))
                    .isInstanceOf(MediaUploadException.class);
        }
    }

    // ===================== delete =====================

    @Nested
    class Delete {

        @Test
        void shouldSucceed_whenCloudinaryReturnsOk() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(eq("campuslink-test/abc123"), any())).thenReturn(Map.of("result", "ok"));

            assertThatCode(() -> mediaService.delete("campuslink-test/abc123")).doesNotThrowAnyException();
        }

        @Test
        void shouldTreatNotFoundAsSuccess_forIdempotency() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(anyString(), any())).thenReturn(Map.of("result", "not found"));

            assertThatCode(() -> mediaService.delete("unknown-id")).doesNotThrowAnyException();
        }

        @Test
        void shouldThrowMediaUploadException_whenCloudinaryReturnsUnexpectedStatus() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(anyString(), any())).thenReturn(Map.of("result", "error"));

            assertThatThrownBy(() -> mediaService.delete("some-id"))
                    .isInstanceOf(MediaUploadException.class);
        }

        @Test
        void shouldThrowMediaUploadException_whenIOExceptionOccurs() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(anyString(), any())).thenThrow(new IOException("Timeout"));

            assertThatThrownBy(() -> mediaService.delete("some-id"))
                    .isInstanceOf(MediaUploadException.class);
        }
    }

    // ===================== replace =====================

    @Nested
    class Replace {

        @Test
        void shouldUploadNewFile_andDeleteOldOne() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenReturn(Map.of(
                    "secure_url", "https://res.cloudinary.com/new.jpg",
                    "public_id", "campuslink-test/new-id"
            ));
            when(uploader.destroy(eq("campuslink-test/old-id"), any())).thenReturn(Map.of("result", "ok"));

            MediaResponse response = mediaService.replace("campuslink-test/old-id", validFile());

            assertThat(response.getPublicId()).isEqualTo("campuslink-test/new-id");
            verify(uploader).destroy(eq("campuslink-test/old-id"), any());
        }

        @Test
        void shouldNotThrow_whenOldFileDeletionFails() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenReturn(Map.of(
                    "secure_url", "https://res.cloudinary.com/new.jpg",
                    "public_id", "campuslink-test/new-id"
            ));
            when(uploader.destroy(eq("campuslink-test/old-id"), any())).thenThrow(new IOException("Timeout"));

            // Le nouveau fichier doit malgré tout être retourné avec succès.
            MediaResponse response = mediaService.replace("campuslink-test/old-id", validFile());

            assertThat(response.getPublicId()).isEqualTo("campuslink-test/new-id");
        }

        @Test
        void shouldSkipDeletion_whenNoExistingPublicId() throws IOException {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), any())).thenReturn(Map.of(
                    "secure_url", "https://res.cloudinary.com/new.jpg",
                    "public_id", "campuslink-test/new-id"
            ));

            mediaService.replace(null, validFile());

            verify(uploader, never()).destroy(any(), any());
        }
    }

    // ===================== mode local (sans Cloudinary) =====================

    @Nested
    class LocalFallback {

        @Test
        void shouldStoreFileLocally_whenCloudinaryNotConfigured() throws IOException {
            mockRequestContext();

            MediaResponse response = localMediaService(java.nio.file.Path.of("target", "uploads-test"))
                    .upload(validFile());

            assertThat(response.getPublicId()).endsWith(".jpg");
            assertThat(response.getUrl()).contains("/media/files/" + response.getPublicId());
            assertThat(response.getBytes()).isEqualTo(validFile().getSize());
        }

        @Test
        void shouldWriteFileToDisk_whenCloudinaryNotConfigured(@org.junit.jupiter.api.io.TempDir Path tempDir)
                throws IOException {
            mockRequestContext();

            MediaResponse response = localMediaService(tempDir).upload(validFile());

            assertThat(Files.exists(tempDir.resolve(response.getPublicId()))).isTrue();
        }

        @Test
        void shouldThrowBadRequest_whenFileInvalid_evenWithoutCloudinary(
                @org.junit.jupiter.api.io.TempDir Path tempDir) {
            MockMultipartFile invalid = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "contenu".getBytes());

            assertThatThrownBy(() -> localMediaService(tempDir).upload(invalid))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void shouldDeleteFile_andStayIdempotent_whenCloudinaryNotConfigured(
                @org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
            Files.write(tempDir.resolve("a.jpg"), "image".getBytes());
            MediaServiceImpl service = localMediaService(tempDir);

            assertThatCode(() -> service.delete("a.jpg")).doesNotThrowAnyException();
            assertThat(Files.exists(tempDir.resolve("a.jpg"))).isFalse();

            // Déjà supprimé : idempotent, pas d'erreur.
            assertThatCode(() -> service.delete("a.jpg")).doesNotThrowAnyException();
        }

        @Test
        void shouldRefusePathTraversal_onDelete(@org.junit.jupiter.api.io.TempDir Path tempDir)
                throws IOException {
            Path outside = tempDir.resolve("..").resolve("outside-secret.txt").normalize();
            Files.write(outside, "secret".getBytes());

            try {
                assertThatCode(() -> localMediaService(tempDir).delete("../outside-secret.txt"))
                        .doesNotThrowAnyException();
                assertThat(Files.exists(outside)).isTrue();
            } finally {
                Files.deleteIfExists(outside);
            }
        }

        @Test
        void shouldFindLocalFile_whenExists(@org.junit.jupiter.api.io.TempDir Path tempDir)
                throws IOException {
            Files.write(tempDir.resolve("b.png"), "image".getBytes());

            assertThat(localMediaService(tempDir).findLocalFile("b.png")).isPresent();
        }

        @Test
        void shouldReturnEmpty_whenFileMissingOrNameInvalid(@org.junit.jupiter.api.io.TempDir Path tempDir) {
            MediaServiceImpl service = localMediaService(tempDir);

            assertThat(service.findLocalFile("missing.png")).isEmpty();
            assertThat(service.findLocalFile("../pom.xml")).isEmpty();
            assertThat(service.findLocalFile("sub/dir.png")).isEmpty();
            assertThat(service.findLocalFile("/etc/passwd")).isEmpty();
        }
    }

}
