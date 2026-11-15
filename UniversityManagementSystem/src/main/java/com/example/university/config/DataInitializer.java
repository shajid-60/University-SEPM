package com.example.university.config;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserRepository repository, PasswordEncoder encoder) {
        return args -> {
            addIfMissing(repository, encoder, "System Admin", "admin@university.com", "admin123", Role.ADMIN, "Administration");
            addIfMissing(repository, encoder, "Demo Teacher", "teacher@university.com", "teacher123", Role.TEACHER, "Computer Science");
            addIfMissing(repository, encoder, "Demo Student", "student@university.com", "student123", Role.STUDENT, "Computer Science");
        };
    }
    private void addIfMissing(UserRepository repository, PasswordEncoder encoder, String name,
                              String email, String password, Role role, String department) {
        if (!repository.existsByEmail(email)) {
            repository.save(new UserAccount(name, email, encoder.encode(password), role, department));
        }
    }
}
