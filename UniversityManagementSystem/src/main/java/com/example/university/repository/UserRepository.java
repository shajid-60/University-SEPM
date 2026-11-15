package com.example.university.repository;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findAllByRole(Role role);
    boolean existsByEmail(String email);
}
