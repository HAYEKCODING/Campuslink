package com.campuslink.entity;

import com.campuslink.enums.Gender;
import com.campuslink.enums.StudyLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Informations personnelles et académiques d'un utilisateur.
 *
 * <p>Table : {@code profiles}. Relation One-to-One avec {@link User}
 * (côté propriétaire : {@code profiles.user_id} porte la clé étrangère,
 * contrainte {@code UNIQUE}).</p>
 */
@Entity
@Table(
        name = "profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_profiles_user_id", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_profiles_phone_number", columnNames = "phone_number")
        },
        indexes = {
                @Index(name = "idx_profiles_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class Profile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_profiles_user"))
    @NotNull(message = "Le profil doit être rattaché à un utilisateur.")
    private User user;

    @ToString.Include
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @ToString.Include
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Past(message = "La date de naissance doit être dans le passé.")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private Gender gender;

    /**
     * Niveau d'étude (Licence / Master / Doctorat) — collecté à l'onboarding
     * (étape 1). Colonne ajoutée après coup : {@code ddl-auto: update} la
     * crée automatiquement sur les bases existantes, {@code database/schema.sql}
     * l'inclut pour les installations neuves.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private StudyLevel level;

    @Size(max = 1000, message = "La bio ne doit pas dépasser 1000 caractères.")
    @Column(name = "bio", length = 1000)
    private String bio;

    /**
     * URL de l'avatar hébergé sur Cloudinary.
     */
    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Le numéro de téléphone est invalide.")
    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Size(max = 150, message = "Le nom de l'établissement ne doit pas dépasser 150 caractères.")
    @Column(name = "university", length = 150)
    private String university;

    @Size(max = 150, message = "La filière ne doit pas dépasser 150 caractères.")
    @Column(name = "field_of_study", length = 150)
    private String fieldOfStudy;

    @Min(value = 1900, message = "L'année de promotion doit être postérieure à 1900.")
    @Max(value = 2100, message = "L'année de promotion doit être antérieure à 2100.")
    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères.")
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100, message = "Le quartier ne doit pas dépasser 100 caractères.")
    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    /**
     * Centres d'intérêt (tags libres). Stockés dans une table de collection
     * dédiée ({@code profile_interests}) plutôt qu'en chaîne concaténée :
     * chaque valeur reste indexable et validable individuellement.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "profile_interests",
            joinColumns = @JoinColumn(name = "profile_id", foreignKey = @ForeignKey(name = "fk_profile_interests_profile")),
            uniqueConstraints = @UniqueConstraint(name = "uk_profile_interests", columnNames = {"profile_id", "interest"})
    )
    @Column(name = "interest", length = 50, nullable = false)
    @Builder.Default
    private Set<String> interests = new HashSet<>();

}
