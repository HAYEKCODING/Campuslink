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
 * <p>Utilise une implémentation anonyme plutôt que le mapper généré par
 * MapStruct (indisponible sans compilation Maven complète) : {@code dateOfBirthToAge}
 * est une méthode {@code default} autonome, testable indépendamment.</p>
 */
class ProfileMapperTest {

    private final ProfileMapper mapper = new ProfileMapper() {
        @Override
        public ProfileResponse toProfileResponse(Profile profile) {
            throw new UnsupportedOperationException("Non exercé par ce test.");
        }

        @Override
        public void updateEntityFromRequest(ProfileRequest request, Profile profile) {
            throw new UnsupportedOperationException("Non exercé par ce test.");
        }
    };

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

}
