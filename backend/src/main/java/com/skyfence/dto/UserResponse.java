package com.skyfence.dto;

import com.skyfence.model.User;

public record UserResponse(Long id, String username, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name());
    }
}
