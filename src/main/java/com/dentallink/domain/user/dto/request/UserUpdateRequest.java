package com.dentallink.domain.user.dto.request;

public record UserUpdateRequest(
        String username,
        String email,
        String password
) {
    public UserUpdateRequest of(String username, String email, String password) {
        return new UserUpdateRequest(username, email, password);
    }
}
