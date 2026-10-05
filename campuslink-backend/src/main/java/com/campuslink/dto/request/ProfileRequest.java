package com.campuslink.dto.request;

import com.campuslink.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

/**
 * Payload de création/modification d'un profil — {@code POST /profiles/me}
 * et {@code PUT /profiles/me}.
 *
 * <p>Un seul DTO pour les deux opérations : les champs éditables et leurs
 * règles de validation sont strictement identiques entre création et mise à
 * jour (PUT y est un remplacement complet, pas un patch partiel).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    @Size(max = 500, message = "L'URL de la photo ne doit pas dépasser 500 caractères.")
    private String avatarUrl;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String lastName;

    /**
     * Genre déclaré — déjà persisté par {@code Profile.gender}, mais absent du DTO :
     * la valeur collectée à l'onboarding était perdue silencieusement.
     */
    private Gender gender;

    /**
     * Utilisée uniquement pour calculer l'âge exposé dans {@code ProfileResponse}
     * — jamais renvoyée telle quelle (voir {@code ProfileMapper}).
     */
    @Past(message = "La date de naissance doit être dans le passé.")
    private LocalDate dateOfBirth;

    @Size(max = 150, message = "Le nom de l'établissement ne doit pas dépasser 150 caractères.")
    private String university;

    @Size(max = 150, message = "La filière ne doit pas dépasser 150 caractères.")
    private String fieldOfStudy;

    @Size(max = 100, message = "Le quartier ne doit pas dépasser 100 caractères.")
    private String neighborhood;

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    private String city;

    @Size(max = 1000, message = "La bio ne doit pas dépasser 1000 caractères.")
    private String bio;

    @Size(max = 20, message = "Vous ne pouvez renseigner plus de 20 centres d'intérêt.")
    private Set<@Size(max = 50, message = "Un centre d'intérêt ne doit pas dépasser 50 caractères.") String> interests;

}
