package com.campuslink.controller;

import com.campuslink.dto.response.ApiResponse;
import com.campuslink.dto.response.MediaResponse;
import com.campuslink.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

/**
 * Endpoints du module média (intégration Cloudinary) : upload, remplacement
 * et suppression de fichiers image.
 *
 * <p>{@code publicId} est toujours transmis en paramètre de requête, jamais
 * dans le chemin de l'URL : les identifiants Cloudinary contiennent des
 * slashes (ex. {@code campuslink/profiles/abc123}), incompatibles avec un
 * simple {@code @PathVariable}.</p>
 *
 * <p>Aucune route de ce controller n'est publique (voir
 * {@code SecurityConstants.PUBLIC_ENDPOINTS}) : un utilisateur doit être
 * authentifié pour uploader, remplacer ou supprimer un fichier. Ce module ne
 * vérifie en revanche pas qu'un {@code publicId} "appartient" à l'appelant —
 * cette vérification d'appartenance relève du module métier qui l'utilise
 * (ex. Profile, pour une photo de profil).</p>
 */
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Validated
@Tag(name = "Médias", description = "Upload, remplacement et suppression de fichiers (Cloudinary)")
public class MediaController {

    /**
     * Noms de fichiers locaux acceptés : commencent par un alphanumérique
     * (jamais par {@code .} ni {@code ..}), sans séparateur de chemin.
     */
    private static final Pattern LOCAL_FILENAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Uploader un fichier image",
            description = "Valide (taille, type MIME), compresse et redimensionne automatiquement "
                    + "l'image avant stockage sur Cloudinary (voir application.media.*)."
    )
    public ApiResponse<MediaResponse> upload(@RequestParam("file") MultipartFile file) {
        MediaResponse response = mediaService.upload(file);
        return ApiResponse.success(response, "Fichier uploadé avec succès.");
    }

    @PutMapping(value = "/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Remplacer un fichier existant",
            description = "Uploade le nouveau fichier puis supprime l'ancien. Le nouveau fichier "
                    + "reste en ligne même si la suppression de l'ancien échoue."
    )
    public ApiResponse<MediaResponse> replace(
            @RequestParam("publicId") @NotBlank(message = "Le publicId est obligatoire.") String publicId,
            @RequestParam("file") MultipartFile file) {
        MediaResponse response = mediaService.replace(publicId, file);
        return ApiResponse.success(response, "Fichier remplacé avec succès.");
    }

    @DeleteMapping
    @Operation(summary = "Supprimer un fichier", description = "Opération idempotente.")
    public ApiResponse<Void> delete(
            @RequestParam("publicId") @NotBlank(message = "Le publicId est obligatoire.") String publicId) {
        mediaService.delete(publicId);
        return ApiResponse.success(null, "Fichier supprimé avec succès.");
    }

    /**
     * Sert un fichier du stockage local de repli (mode sans Cloudinary) :
     * route publique (voir {@code SecurityConstants.PUBLIC_ENDPOINTS}) car les
     * avatars doivent s'afficher sur la landing et les profils consultés en
     * navigation anonyme.
     *
     * <p>Le nom de fichier est validé par un motif strict puis re-normalisé :
     * aucune traversée de dossier n'est possible.</p>
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> serveLocalFile(@PathVariable String filename) {
        if (!LOCAL_FILENAME_PATTERN.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }
        return mediaService.findLocalFile(filename)
                .<ResponseEntity<Resource>>map(resource -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(resource))
                .orElseGet(ResponseEntity.notFound()::build);
    }

}
