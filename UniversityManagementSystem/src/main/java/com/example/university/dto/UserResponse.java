package com.example.university.dto;

import com.example.university.model.Role;
import com.example.university.model.UserAccount;

public record UserResponse(Long id, String name, String email, Role role, String department) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getDepartment());
    }
}
