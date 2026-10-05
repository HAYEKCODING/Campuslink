package com.campuslink.specification;

import com.campuslink.dto.request.UserSearchCriteria;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit dynamiquement une {@link Specification} JPA (Criteria API) à
 * partir de {@link UserSearchCriteria} — même approche que
 * {@code ProfileSpecification} : seuls les critères renseignés ajoutent un
 * prédicat, combinés en {@code AND}.
 */
public final class UserSpecification {

    private UserSpecification() {
        // Classe utilitaire : instanciation interdite
    }

    public static Specification<User> withCriteria(UserSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getEmail())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + criteria.getEmail().trim().toLowerCase() + "%"));
            }

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getEmailVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("emailVerified"), criteria.getEmailVerified()));
            }

            if (criteria.getRole() != null) {
                if (query != null) {
                    query.distinct(true);
                }
                Join<User, Role> rolesJoin = root.join("roles", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(rolesJoin.get("name"), criteria.getRole()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
