package com.dentallink.domain.user.service.command;

import com.dentallink.domain.user.dto.request.UserDeleteRequest;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;

public interface UserCommandService {
    UserResponse signup(@Valid UserSignupRequest request);
    UserResponse updateUser(@Valid UserUpdateRequest request, AuthUser authUser);
    Void changePassword(@Valid UserUpdatePasswordRequest request, AuthUser authUser);
    Void withdraw(@Valid UserDeleteRequest request, AuthUser authUser);
}
