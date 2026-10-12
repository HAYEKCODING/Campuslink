package com.campuslink.mapper;

import com.campuslink.dto.request.ProfileRequest;
import com.campuslink.dto.response.ProfileResponse;
import com.campuslink.entity.Profile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de {@link ProfileMapper#dateOfBirthToAge}, la seule logique non
 * triviale de ce mapper. Le reste (mapping champ à champ) est généré par
 * MapStruct et ne nécessite pas de test dédié.
 *
 * <p>Le mapper généré est aussi utilisé pour vérifier la stratégie de fusion
 * des valeurs null lors d'une mise à jour partielle.</p>
 */
class ProfileMapperTest {

    private final ProfileMapper mapper = new ProfileMapperImpl();

    @Test
    void shouldReturnNull_whenDateOfBirthIsNull() {
        assertThat(mapper.dateOfBirthToAge(null)).isNull();
    }

    @Test
    void shouldComputeExactAge_whenBirthdayAlreadyPassedThisYear() {
        LocalDate twentyYearsAgo = LocalDate.now().minusYears(20).minusDays(1);
        assertThat(mapper.dateOfBirthToAge(twentyYearsAgo)).isEqualTo(20);
    }

    @Test
    void shouldNotYetIncrementAge_whenBirthdayNotYetReachedThisYear() {
        LocalDate almostTwentyOne = LocalDate.now().minusYears(21).plusDays(1);
        assertThat(mapper.dateOfBirthToAge(almostTwentyOne)).isEqualTo(20);
    }

    @Test
    void shouldPreserveUnprovidedValues_whenApplyingPartialUpdate() {
        Profile profile = Profile.builder()
                .firstName("Awa")
                .lastName("Kone")
                .bio("Bio existante")
                .interests(java.util.Set.of("Lecture"))
                .build();

        mapper.updateEntityFromRequest(ProfileRequest.builder().build(), profile);

        assertThat(profile.getFirstName()).isEqualTo("Awa");
        assertThat(profile.getLastName()).isEqualTo("Kone");
        assertThat(profile.getBio()).isEqualTo("Bio existante");
        assertThat(profile.getInterests()).containsExactly("Lecture");
    }

    @Test
    void publicProfileMappingDoesNotExposeBirthDate() {
        Profile profile = Profile.builder()
                .firstName("Awa")
                .lastName("Kone")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

        assertThat(mapper.toPublicProfileResponse(profile).getDateOfBirth()).isNull();
    }

}
