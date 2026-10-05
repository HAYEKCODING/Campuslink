package com.campuslink.service;

import com.campuslink.dto.response.MediaResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrat métier du module média (intégration Cloudinary).
 *
 * <p>Générique et indépendant de tout domaine métier (Profile, etc.) : ce
 * service sait uploader, remplacer et supprimer une image, rien de plus. La
 * décision de "quel fichier appartient à quelle ressource" (association
 * {@code publicId} ↔ profil, événement, etc.) relève des modules appelants.</p>
 */
public interface MediaService {

    /**
     * Valide, compresse et uploade un fichier image.
     *
     * @throws com.campuslink.exception.BadRequestException si le fichier est invalide
     *         (vide, trop volumineux, type non autorisé)
     * @throws com.campuslink.exception.MediaUploadException si l'upload échoue côté Cloudinary
     */
    MediaResponse upload(MultipartFile file);

    /**
     * Uploade un nouveau fichier puis supprime l'ancien (au mieux — un échec
     * de suppression de l'ancien ne fait pas échouer l'opération, voir
     * l'implémentation).
     *
     * @param existingPublicId identifiant Cloudinary du fichier à remplacer
     *                          (peut être {@code null} ou vide s'il n'y en avait pas)
     */
    MediaResponse replace(String existingPublicId, MultipartFile newFile);

    /**
     * Supprime un fichier. Idempotent : un {@code publicId} déjà supprimé ou
     * inconnu de Cloudinary n'est pas traité comme une erreur.
     *
     * @throws com.campuslink.exception.MediaUploadException si la suppression échoue réellement
     */
    void delete(String publicId);

}
