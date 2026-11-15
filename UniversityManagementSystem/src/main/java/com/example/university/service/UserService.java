package com.example.university.service;

import com.example.university.dto.UserRequest;
import com.example.university.dto.UserResponse;
import com.example.university.model.Role;
import com.example.university.model.UserAccount;
import com.example.university.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository; this.encoder = encoder;
    }

    public List<UserResponse> findAll(Role role) {
        return repository.findAllByRole(role).stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(Long id, Role role) {
        return UserResponse.from(getUser(id, role));
    }

    public UserResponse create(UserRequest request, Role role) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        UserAccount user = new UserAccount(request.getName(), request.getEmail(),
                encoder.encode(request.getPassword()), role, request.getDepartment());
        return UserResponse.from(repository.save(user));
    }

    public UserResponse update(Long id, UserRequest request, Role role) {
        UserAccount user = getUser(id, role);
        repository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(encoder.encode(request.getPassword()));
        }
        return UserResponse.from(repository.save(user));
    }

    public void delete(Long id, Role role) {
        repository.delete(getUser(id, role));
    }

    private UserAccount getUser(Long id, Role role) {
        UserAccount user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != role) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        return user;
    }
}
