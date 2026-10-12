package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
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
 * Message déposé via le formulaire de contact public de la landing page —
 * alimente {@code POST /contact}, endpoint public.
 *
 * <p>Table : {@code contact_messages}. « file d'attente » plutôt qu'envoi
 * email : le backend n'a pas toujours de SMTP configuré (voir
 * {@code spring.mail} et l'indicateur de santé mail), la persistance garantit
 * donc qu'aucun message n'est perdu. Un administrateur traite ensuite la
 * table via {@code handled}.</p>
 */
@Entity
@Table(
        name = "contact_messages",
        indexes = {
                @Index(name = "idx_contact_messages_created_at", columnList = "created_at"),
                @Index(name = "idx_contact_messages_handled", columnList = "handled")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class ContactMessage extends BaseEntity {

    @ToString.Include
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ToString.Include
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Le format de l'email est invalide.")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères.")
    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @NotBlank(message = "Le message est obligatoire.")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères.")
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    /** Traité (répondu) par un administrateur ? */
    @Column(name = "handled", nullable = false)
    @Builder.Default
    private boolean handled = false;

}
