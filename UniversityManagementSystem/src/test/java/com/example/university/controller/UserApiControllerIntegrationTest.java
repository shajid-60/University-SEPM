package com.example.university.controller;

import com.example.university.model.Role;
import com.example.university.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the whole request path of the teacher REST resource:
 * HTTP -> Spring Security -> UserApiController -> UserService -> JPA -> database.
 *
 * The test is transactional, so every row it writes is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "admin@university.com", roles = "ADMIN")
    @DisplayName("An admin can create, read, update and delete a teacher through the REST API")
    void adminCanRunTheFullTeacherLifecycle() throws Exception {
        String createPayload = """
                {"name":"Nadia Islam","email":"nadia@university.com","password":"teacher123","department":"Computer Science"}
                """;

        // CREATE -> 201 Created, and the password must never be echoed back.
        String created = mockMvc.perform(post("/api/teachers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Nadia Islam"))
                .andExpect(jsonPath("$.email").value("nadia@university.com"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.department").value("Computer Science"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        long id = repository.findByEmail("nadia@university.com").orElseThrow().getId();
        assertThat(created).contains("\"id\":" + id);

        // The password really was hashed on the way into the database.
        String storedHash = repository.findByEmail("nadia@university.com").orElseThrow().getPassword();
        assertThat(storedHash).isNotEqualTo("teacher123");
        assertThat(passwordEncoder.matches("teacher123", storedHash)).isTrue();

        // READ one -> 200 OK.
        mockMvc.perform(get("/api/teachers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nadia@university.com"));

        // READ many -> the new teacher is part of the teacher list.
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'nadia@university.com')]").exists());

        // The teacher must not leak into another role's collection.
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'nadia@university.com')]").doesNotExist());

        // UPDATE with a blank password -> 200 OK, fields change, password is kept.
        String updatePayload = """
                {"name":"Nadia Islam Chowdhury","email":"nadia.c@university.com","password":"","department":"Software Engineering"}
                """;
        mockMvc.perform(put("/api/teachers/" + id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nadia Islam Chowdhury"))
                .andExpect(jsonPath("$.email").value("nadia.c@university.com"))
                .andExpect(jsonPath("$.department").value("Software Engineering"));

        var updated = repository.findById(id).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(Role.TEACHER);
        assertThat(passwordEncoder.matches("teacher123", updated.getPassword())).isTrue();

        // DELETE -> 204 No Content, and the row is gone.
        mockMvc.perform(delete("/api/teachers/" + id).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teachers/" + id)).andExpect(status().isNotFound());
        assertThat(repository.findById(id)).isEmpty();
    }
}
