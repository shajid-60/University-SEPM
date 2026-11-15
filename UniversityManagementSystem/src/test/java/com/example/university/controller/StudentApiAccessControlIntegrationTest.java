package com.example.university.controller;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.assertj.core.api.SoftAssertions;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Integration test for the role rules on the student REST resource.
 *
 * A student signs in to look up the student directory, so GET must be allowed.
 * Creating, editing and deleting student records is administrative work, so
 * those requests must be rejected with 403 Forbidden for a STUDENT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentApiAccessControlIntegrationTest {

    private static final int OK = 200;
    private static final int FORBIDDEN = 403;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "student@university.com", roles = "STUDENT")
    @DisplayName("A student may read the student directory but may not modify any student record")
    void aStudentHasReadOnlyAccessToTheStudentResource() throws Exception {
        // A classmate's record, owned by somebody other than the signed-in student.
        UserAccount classmate = repository.save(new UserAccount("Tanvir Ahmed", "tanvir@university.com",
                passwordEncoder.encode("student123"), Role.STUDENT, "Computer Science"));
        long classmateId = classmate.getId();

        int listStatus = mockMvc.perform(get("/api/students"))
                .andReturn().getResponse().getStatus();

        int readOneStatus = mockMvc.perform(get("/api/students/" + classmateId))
                .andReturn().getResponse().getStatus();

        int createStatus = mockMvc.perform(post("/api/students").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fake Enrolment","email":"fake@university.com","password":"pass123","department":"Computer Science"}
                                """))
                .andReturn().getResponse().getStatus();

        // Changing a classmate's email and password would be a full account takeover.
        int updateStatus = mockMvc.perform(put("/api/students/" + classmateId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tanvir Ahmed","email":"attacker@university.com","password":"hijacked123","department":"Computer Science"}
                                """))
                .andReturn().getResponse().getStatus();

        int deleteStatus = mockMvc.perform(delete("/api/students/" + classmateId).with(csrf()))
                .andReturn().getResponse().getStatus();

        SoftAssertions.assertSoftly(checks -> {
            checks.assertThat(listStatus)
                    .as("a student should be able to list the student directory (GET /api/students)")
                    .isEqualTo(OK);
            checks.assertThat(readOneStatus)
                    .as("a student should be able to open one student record (GET /api/students/{id})")
                    .isEqualTo(OK);
            checks.assertThat(createStatus)
                    .as("a student must NOT be able to enrol new students (POST /api/students)")
                    .isEqualTo(FORBIDDEN);
            checks.assertThat(updateStatus)
                    .as("a student must NOT be able to change a classmate's email and password (PUT /api/students/{id})")
                    .isEqualTo(FORBIDDEN);
            checks.assertThat(deleteStatus)
                    .as("a student must NOT be able to delete a classmate's record (DELETE /api/students/{id})")
                    .isEqualTo(FORBIDDEN);
        });
    }
}
