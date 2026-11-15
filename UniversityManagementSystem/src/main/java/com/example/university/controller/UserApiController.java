package com.example.university.controller;

import com.example.university.dto.UserRequest;
import com.example.university.dto.UserResponse;
import com.example.university.model.Role;
import com.example.university.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserApiController {
    private final UserService service;
    public UserApiController(UserService service) { this.service = service; }

    @GetMapping("/admins") public List<UserResponse> admins() { return service.findAll(Role.ADMIN); }
    @GetMapping("/admins/{id}") public UserResponse admin(@PathVariable Long id) { return service.findById(id, Role.ADMIN); }
    @PostMapping("/admins") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAdmin(@Valid @RequestBody UserRequest request) { return service.create(request, Role.ADMIN); }
    @PutMapping("/admins/{id}") public UserResponse updateAdmin(@PathVariable Long id, @Valid @RequestBody UserRequest request) { return service.update(id, request, Role.ADMIN); }
    @DeleteMapping("/admins/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdmin(@PathVariable Long id) { service.delete(id, Role.ADMIN); }

    @GetMapping("/teachers") public List<UserResponse> teachers() { return service.findAll(Role.TEACHER); }
    @GetMapping("/teachers/{id}") public UserResponse teacher(@PathVariable Long id) { return service.findById(id, Role.TEACHER); }
    @PostMapping("/teachers") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createTeacher(@Valid @RequestBody UserRequest request) { return service.create(request, Role.TEACHER); }
    @PutMapping("/teachers/{id}") public UserResponse updateTeacher(@PathVariable Long id, @Valid @RequestBody UserRequest request) { return service.update(id, request, Role.TEACHER); }
    @DeleteMapping("/teachers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacher(@PathVariable Long id) { service.delete(id, Role.TEACHER); }

    @GetMapping("/students") public List<UserResponse> students() { return service.findAll(Role.STUDENT); }
    @GetMapping("/students/{id}") public UserResponse student(@PathVariable Long id) { return service.findById(id, Role.STUDENT); }
    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createStudent(@Valid @RequestBody UserRequest request) { return service.create(request, Role.STUDENT); }
    @PutMapping("/students/{id}") public UserResponse updateStudent(@PathVariable Long id, @Valid @RequestBody UserRequest request) { return service.update(id, request, Role.STUDENT); }
    @DeleteMapping("/students/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) { service.delete(id, Role.STUDENT); }
}
