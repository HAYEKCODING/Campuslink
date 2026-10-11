package com.campuslink.specification;

import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.entity.Profile;
import com.campuslink.entity.User;
import com.campuslink.enums.Gender;
import com.campuslink.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de {@link ProfileSpecification} contre une base H2 réelle
 * (via {@link DataJpaTest}) : vérifie le comportement effectif des prédicats
 * générés par la Criteria API, plutôt que de mocker {@code Root}/{@code CriteriaBuilder}
 * (ce qui ne testerait que la construction de l'objet, pas le résultat réel de la requête).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProfileSpecificationIntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Profile persistProfile(String firstName, int age, String university, String city, String... interests) {
        User user = User.builder()
                .email(firstName.toLowerCase() + "@campuslink.io")
                .password("hashed-password")
                .build();
        entityManager.persist(user);

        Profile profile = Profile.builder()
                .firstName(firstName)
                .lastName("Test")
                .dateOfBirth(LocalDate.now().minusYears(age))
                .university(university)
                .city(city)
                .interests(interests.length > 0 ? new HashSet<>(Set.of(interests)) : new HashSet<>())
                .build();
        user.setProfile(profile);
        entityManager.persist(profile);

        return profile;
    }

    @Test
    void shouldReturnAllProfiles_whenNoCriteriaProvided() {
        persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 30, "INP-HB", "Yamoussoukro");
        entityManager.flush();

        Specification<Profile> specification = ProfileSpecification.withCriteria(ProfileSearchCriteria.builder().build());
        List<Profile> results = profileRepository.findAll(specification);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFilterByAgeRange() {
        persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 30, "UFHB", "Bouake");
        persistProfile("Fatou", 25, "UFHB", "Bouake");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().minAge(22).maxAge(28).build();
        Specification<Profile> specification = ProfileSpecification.withCriteria(criteria);

        List<Profile> results = profileRepository.findAll(specification);

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Fatou");
    }

    @Test
    void shouldExcludeCurrentUserProfile_whenExcludedUserIdProvided() {
        Profile awa = persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 22, "UFHB", "Bouake");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .excludedUserId(awa.getUser().getId())
                .build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        // Le profil de l'utilisateur courant ne figure pas dans les résultats
        // (feed de découverte / recherche : on ne se voit pas soi-même).
        assertThat(results).extracting(Profile::getFirstName).containsExactly("Moussa");
    }

    @Test
    void shouldIncludeAllProfiles_whenExcludedUserIdIsNull() {
        persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 22, "UFHB", "Bouake");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().excludedUserId(null).build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFilterByMinAgeOnly() {
        persistProfile("Awa", 18, "UFHB", "Bouake");
        persistProfile("Moussa", 25, "UFHB", "Bouake");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().minAge(20).build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Moussa");
    }

    @Test
    void shouldFilterByUniversity_caseInsensitivePartialMatch() {
        persistProfile("Awa", 20, "UFHB Bouake", "Bouake");
        persistProfile("Moussa", 22, "INP-HB Yamoussoukro", "Yamoussoukro");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().university("ufhb").build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Awa");
    }

    @Test
    void shouldFilterByCity() {
        persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 22, "INP-HB", "Yamoussoukro");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().city("Yamoussoukro").build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Moussa");
    }

    @Test
    void shouldFilterByInterests_matchingAnyOne() {
        persistProfile("Awa", 20, "UFHB", "Bouake", "Football", "Musique");
        persistProfile("Moussa", 22, "UFHB", "Bouake", "Lecture");
        persistProfile("Fatou", 24, "UFHB", "Bouake", "Football");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .interests(Set.of("Football"))
                .build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactlyInAnyOrder("Awa", "Fatou");
    }

    @Test
    void shouldNotDuplicateProfile_whenMatchingMultipleRequestedInterests() {
        // Awa a les deux centres d'intérêt demandés : sans distinct(true), la
        // jointure produirait deux lignes pour ce même profil.
        persistProfile("Awa", 20, "UFHB", "Bouake", "Football", "Musique");
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .interests(Set.of("Football", "Musique"))
                .build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).hasSize(1);
    }

    @Test
    void shouldFilterByGender() {
        persistProfile("Awa", 20, "UFHB", "Bouake").setGender(Gender.FEMALE);
        persistProfile("Moussa", 22, "UFHB", "Bouake").setGender(Gender.MALE);
        persistProfile("Fatou", 24, "UFHB", "Bouake").setGender(Gender.FEMALE);
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().gender(Gender.FEMALE).build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactlyInAnyOrder("Awa", "Fatou");
    }

    @Test
    void shouldReturnAllProfiles_whenGenderIsNull() {
        // gender null = critère absent : aucun filtre appliqué (y compris
        // pour les profils dont le genre est eux-mêmes null).
        persistProfile("Awa", 20, "UFHB", "Bouake");
        persistProfile("Moussa", 22, "UFHB", "Bouake").setGender(Gender.MALE);
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder().build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldCombineGenderWithOtherFilters() {
        persistProfile("Awa", 20, "UFHB", "Bouake").setGender(Gender.FEMALE);
        persistProfile("Fatou", 24, "INP-HB", "Yamoussoukro").setGender(Gender.FEMALE);
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .gender(Gender.FEMALE)
                .city("Bouake")
                .build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Awa");
    }

    @Test
    void shouldCombineMultipleFilters_withAndSemantics() {
        persistProfile("Awa", 20, "UFHB", "Bouake", "Football");
        persistProfile("Moussa", 20, "UFHB", "Yamoussoukro", "Football"); // mauvaise ville
        entityManager.flush();

        ProfileSearchCriteria criteria = ProfileSearchCriteria.builder()
                .city("Bouake")
                .interests(Set.of("Football"))
                .build();
        List<Profile> results = profileRepository.findAll(ProfileSpecification.withCriteria(criteria));

        assertThat(results).extracting(Profile::getFirstName).containsExactly("Awa");
    }

}
