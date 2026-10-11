package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Témoignage affiché sur la landing page (section « Ce que dit la
 * communauté ») — alimente {@code GET /testimonials}, endpoint public.
 *
 * <p>Table : {@code testimonials}. Contenu éditorial géré par les
 * administrateurs (aucune écriture publique) : {@code active} permet de
 * masquer un témoignage sans le supprimer, {@code displayOrder} contrôle
 * l'ordre d'affichage.</p>
 */
@Entity
@Table(
        name = "testimonials",
        indexes = {
                @Index(name = "idx_testimonials_active_order", columnList = "active, display_order")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class Testimonial extends BaseEntity {

    @ToString.Include
    @NotBlank(message = "Le nom de l'auteur du témoignage est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Fonction / rôle affiché sous le nom (ex. « Étudiante en médecine »). */
    @Size(max = 100, message = "Le rôle ne doit pas dépasser 100 caractères.")
    @Column(name = "role", length = 100)
    private String role;

    /** URL de la photo d'illustration (Cloudinary) — optionnelle. */
    @Size(max = 500, message = "L'URL de la photo ne doit pas dépasser 500 caractères.")
    @Column(name = "photo", length = 500)
    private String photo;

    @ToString.Include
    @NotBlank(message = "Le texte du témoignage est obligatoire.")
    @Size(max = 1000, message = "Le témoignage ne doit pas dépasser 1000 caractères.")
    @Column(name = "quote", nullable = false, length = 1000)
    private String quote;

    /** Ordre d'affichage croissant (les plus petits d'abord). */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** Un témoignage inactif n'est jamais renvoyé par l'endpoint public. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

}
