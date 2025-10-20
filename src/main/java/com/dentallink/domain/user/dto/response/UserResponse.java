package com.dentallink.domain.user.dto.response;

import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record UserResponse (
    Long userId,
    String username,
    String email,
    UserRole role,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserRole(),
                user.getCreatedAt()
        );
    }
}
