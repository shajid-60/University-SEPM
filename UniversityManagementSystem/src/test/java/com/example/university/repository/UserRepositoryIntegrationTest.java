package com.example.university.repository;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Spring Data JPA layer.
 *
 * Unlike the unit tests, this one boots the JPA slice of the application and
 * runs the derived query methods against a real (in-memory) database, so the
 * generated SQL and the entity mapping are both exercised.
 */
@DataJpaTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("Derived query methods read back persisted accounts and filter by role")
    void derivedQueriesWorkAgainstARealDatabase() {
        repository.saveAll(List.of(
                new UserAccount("Rafiq Hasan", "rafiq@university.com", "$2a$10$adminhash", Role.ADMIN, "Administration"),
                new UserAccount("Nadia Islam", "nadia@university.com", "$2a$10$teacherhash", Role.TEACHER, "Computer Science"),
                new UserAccount("Tanvir Ahmed", "tanvir@university.com", "$2a$10$studenthash", Role.STUDENT, "Computer Science"),
                new UserAccount("Sumaiya Akter", "sumaiya@university.com", "$2a$10$studenthash2", Role.STUDENT, "Mathematics")));
        repository.flush();

        // findByEmail returns the whole mapped entity, including the generated id.
        Optional<UserAccount> found = repository.findByEmail("nadia@university.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getName()).isEqualTo("Nadia Islam");
        assertThat(found.get().getRole()).isEqualTo(Role.TEACHER);
        assertThat(found.get().getDepartment()).isEqualTo("Computer Science");

        // findByEmail must not match an address that was never stored.
        assertThat(repository.findByEmail("shajid@university.com")).isEmpty();

        // findAllByRole must return only the rows of the requested role.
        assertThat(repository.findAllByRole(Role.STUDENT))
                .extracting(UserAccount::getEmail)
                .containsExactlyInAnyOrder("tanvir@university.com", "sumaiya@university.com");
        assertThat(repository.findAllByRole(Role.ADMIN))
                .extracting(UserAccount::getEmail)
                .containsExactly("rafiq@university.com");

        // existsByEmail backs the duplicate-email check in UserService.
        assertThat(repository.existsByEmail("tanvir@university.com")).isTrue();
        assertThat(repository.existsByEmail("missing@university.com")).isFalse();
    }
}
