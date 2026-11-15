package com.example.university.service;

import com.example.university.dto.UserRequest;
import com.example.university.dto.UserResponse;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CRUD rules in {@link UserService}.
 *
 * These are plain unit tests: the repository and the password encoder are
 * mocked, so no Spring context and no database are started.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService service;

    private UserRequest request(String name, String email, String password, String department) {
        UserRequest request = new UserRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setDepartment(department);
        return request;
    }

    private UserAccount account(Long id, String name, String email, String password, Role role, String department) {
        UserAccount account = new UserAccount(name, email, password, role, department);
        try {
            var field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set the test id", e);
        }
        return account;
    }

    @Test
    @DisplayName("create() stores the password hashed and never in plain text")
    void createHashesThePasswordBeforeSaving() {
        when(repository.existsByEmail("ayesha@university.com")).thenReturn(false);
        when(encoder.encode("pass123")).thenReturn("$2a$10$hashedvalue");
        when(repository.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));

        service.create(request("Ayesha Rahman", "ayesha@university.com", "pass123", "Computer Science"), Role.STUDENT);

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("$2a$10$hashedvalue");
        assertThat(saved.getValue().getPassword()).isNotEqualTo("pass123");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("create() rejects an email that is already taken with 409 CONFLICT")
    void createRejectsDuplicateEmail() {
        when(repository.existsByEmail("admin@university.com")).thenReturn(true);

        assertThatThrownBy(() ->
                service.create(request("Copy Admin", "admin@university.com", "pass123", "Administration"), Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already exists")
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(repository, never()).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("create() rejects a blank password with 400 BAD_REQUEST")
    void createRejectsBlankPassword() {
        when(repository.existsByEmail("nopassword@university.com")).thenReturn(false);

        assertThatThrownBy(() ->
                service.create(request("No Password", "nopassword@university.com", "   ", "Physics"), Role.TEACHER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password is required")
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repository, never()).save(any(UserAccount.class));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("findById() hides a record that belongs to a different role behind 404 NOT_FOUND")
    void findByIdDoesNotLeakRecordsFromAnotherRole() {
        UserAccount admin = account(1L, "System Admin", "admin@university.com", "hash", Role.ADMIN, "Administration");
        when(repository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.findById(1L, Role.STUDENT))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found")
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("update() keeps the stored password when the request leaves the password blank")
    void updateKeepsTheExistingPasswordWhenBlank() {
        UserAccount student = account(7L, "Old Name", "old@university.com", "$2a$10$originalhash", Role.STUDENT, "Physics");
        when(repository.findById(7L)).thenReturn(Optional.of(student));
        when(repository.findByEmail("new@university.com")).thenReturn(Optional.empty());
        when(repository.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = service.update(7L, request("New Name", "new@university.com", "", "Mathematics"), Role.STUDENT);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@university.com");
        assertThat(response.department()).isEqualTo("Mathematics");
        assertThat(student.getPassword()).isEqualTo("$2a$10$originalhash");
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("findAll() maps only the requested role and never exposes the password")
    void findAllMapsTheRequestedRoleWithoutPasswords() {
        when(repository.findAllByRole(Role.TEACHER)).thenReturn(List.of(
                account(2L, "Demo Teacher", "teacher@university.com", "$2a$10$teacherhash", Role.TEACHER, "Computer Science"),
                account(3L, "Second Teacher", "second@university.com", "$2a$10$secondhash", Role.TEACHER, "Mathematics")));

        List<UserResponse> teachers = service.findAll(Role.TEACHER);

        assertThat(teachers).hasSize(2);
        assertThat(teachers).extracting(UserResponse::email)
                .containsExactly("teacher@university.com", "second@university.com");
        assertThat(teachers).allSatisfy(teacher -> assertThat(teacher.role()).isEqualTo(Role.TEACHER));
        assertThat(teachers.toString()).doesNotContain("hash");
    }
}
