package com.campuslink.specification;

import com.campuslink.dto.request.UserSearchCriteria;
import com.campuslink.entity.Role;
import com.campuslink.entity.User;
import com.campuslink.enums.AccountStatus;
import com.campuslink.enums.RoleName;
import com.campuslink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de {@link UserSpecification} contre une base H2 réelle —
 * même approche que {@code ProfileSpecificationIntegrationTest} : vérifie le
 * comportement effectif des prédicats, pas seulement leur construction.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserSpecificationIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role persistRole(RoleName name) {
        Role role = Role.builder().name(name).build();
        entityManager.persist(role);
        return role;
    }

    private User persistUser(String email, AccountStatus status, boolean emailVerified, Role... roles) {
        User user = User.builder()
                .email(email)
                .password("hashed-password")
                .status(status)
                .emailVerified(emailVerified)
                .build();
        for (Role role : roles) {
            user.addRole(role);
        }
        entityManager.persist(user);
        return user;
    }

    @Test
    void shouldReturnAllUsers_whenNoCriteriaProvided() {
        Role studentRole = persistRole(RoleName.STUDENT);
        persistUser("awa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        persistUser("moussa@campuslink.io", AccountStatus.PENDING_VERIFICATION, false, studentRole);
        entityManager.flush();

        List<User> results = userRepository.findAll(UserSpecification.withCriteria(UserSearchCriteria.builder().build()));

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFilterByEmail_caseInsensitivePartialMatch() {
        Role studentRole = persistRole(RoleName.STUDENT);
        persistUser("awa.kone@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        persistUser("moussa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder().email("KONE").build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).extracting(User::getEmail).containsExactly("awa.kone@campuslink.io");
    }

    @Test
    void shouldFilterByStatus() {
        Role studentRole = persistRole(RoleName.STUDENT);
        persistUser("awa@campuslink.io", AccountStatus.SUSPENDED, true, studentRole);
        persistUser("moussa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder().status(AccountStatus.SUSPENDED).build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).extracting(User::getEmail).containsExactly("awa@campuslink.io");
    }

    @Test
    void shouldFilterByEmailVerified() {
        Role studentRole = persistRole(RoleName.STUDENT);
        persistUser("awa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        persistUser("moussa@campuslink.io", AccountStatus.PENDING_VERIFICATION, false, studentRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder().emailVerified(false).build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).extracting(User::getEmail).containsExactly("moussa@campuslink.io");
    }

    @Test
    void shouldFilterByRole() {
        Role studentRole = persistRole(RoleName.STUDENT);
        Role adminRole = persistRole(RoleName.ADMIN);
        persistUser("awa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        persistUser("admin@campuslink.io", AccountStatus.ACTIVE, true, adminRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder().role(RoleName.ADMIN).build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).extracting(User::getEmail).containsExactly("admin@campuslink.io");
    }

    @Test
    void shouldNotDuplicateUser_whenMultiRoleAndFilteredByOneOfThem() {
        Role studentRole = persistRole(RoleName.STUDENT);
        Role teacherRole = persistRole(RoleName.TEACHER);
        persistUser("awa@campuslink.io", AccountStatus.ACTIVE, true, studentRole, teacherRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder().role(RoleName.STUDENT).build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).hasSize(1);
    }

    @Test
    void shouldCombineMultipleFilters_withAndSemantics() {
        Role studentRole = persistRole(RoleName.STUDENT);
        persistUser("awa@campuslink.io", AccountStatus.ACTIVE, true, studentRole);
        persistUser("moussa@campuslink.io", AccountStatus.SUSPENDED, true, studentRole);
        entityManager.flush();

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .status(AccountStatus.ACTIVE)
                .role(RoleName.STUDENT)
                .build();
        List<User> results = userRepository.findAll(UserSpecification.withCriteria(criteria));

        assertThat(results).extracting(User::getEmail).containsExactly("awa@campuslink.io");
    }

}
