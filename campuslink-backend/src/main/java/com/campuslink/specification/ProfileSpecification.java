package com.campuslink.specification;

import com.campuslink.dto.request.ProfileSearchCriteria;
import com.campuslink.entity.Profile;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Construit dynamiquement une {@link Specification} JPA (Criteria API) à
 * partir de {@link ProfileSearchCriteria} : seuls les critères effectivement
 * renseignés ajoutent un prédicat à la requête finale (combinés en {@code AND}).
 *
 * <p>Classe utilitaire statique — pas de dépendance, testable indépendamment
 * (voir {@code ProfileSpecificationIntegrationTest}, qui vérifie le
 * comportement réel des prédicats générés contre une base H2).</p>
 */
public final class ProfileSpecification {

    private ProfileSpecification() {
        // Classe utilitaire : instanciation interdite
    }

    public static Specification<Profile> withCriteria(ProfileSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ===================== Âge (traduit en bornes de date de naissance) =====================
            // L'entité ne porte pas de colonne "âge" (voir Profile.dateOfBirth et
            // ProfileMapper.dateOfBirthToAge) : le filtre se traduit donc en comparaison
            // de dates. "âge >= minAge" équivaut à "né(e) le, ou avant, la date à laquelle
            // on atteint exactement minAge aujourd'hui".
            if (criteria.getMinAge() != null) {
                LocalDate latestBirthDateForMinAge = LocalDate.now().minusYears(criteria.getMinAge());
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("dateOfBirth"), latestBirthDateForMinAge));
            }

            if (criteria.getMaxAge() != null) {
                // "âge <= maxAge" équivaut à "né(e) strictement après la date à laquelle
                // on atteint maxAge + 1 an" (sinon on aurait déjà maxAge + 1 ans révolus).
                LocalDate earliestBirthDateForMaxAge =
                        LocalDate.now().minusYears(criteria.getMaxAge() + 1L).plusDays(1);
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("dateOfBirth"), earliestBirthDateForMaxAge));
            }

            // ===================== Champs texte libres (contient, insensible à la casse) =====================
            addContainsPredicate(predicates, criteriaBuilder, root.get("university"), criteria.getUniversity());
            addContainsPredicate(predicates, criteriaBuilder, root.get("fieldOfStudy"), criteria.getFieldOfStudy());
            addContainsPredicate(predicates, criteriaBuilder, root.get("neighborhood"), criteria.getNeighborhood());
            addContainsPredicate(predicates, criteriaBuilder, root.get("city"), criteria.getCity());

            // ===================== Centres d'intérêt (correspond à au moins un) =====================
            if (criteria.getInterests() != null && !criteria.getInterests().isEmpty()) {
                Set<String> normalizedInterests = criteria.getInterests().stream()
                        .filter(StringUtils::hasText)
                        .map(interest -> interest.trim().toLowerCase())
                        .collect(Collectors.toSet());

                if (!normalizedInterests.isEmpty()) {
                    if (query != null) {
                        // Une jointure sur une collection peut dupliquer les lignes d'un
                        // profil correspondant à plusieurs centres d'intérêt demandés.
                        query.distinct(true);
                    }
                    Join<Profile, String> interestsJoin = root.join("interests", JoinType.INNER);
                    predicates.add(criteriaBuilder.lower(interestsJoin).in(normalizedInterests));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addContainsPredicate(List<Predicate> predicates,
                                               CriteriaBuilder criteriaBuilder,
                                               Path<String> path,
                                               String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(path), "%" + value.trim().toLowerCase() + "%"));
        }
    }

}
