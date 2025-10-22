package com.dentallink.domain.auth.service;

import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.TokenResponse;

public interface AuthService {

    void passwordCheck(String password, Long userId);
    String passwordEncode(String password);
    TokenResponse login(LoginRequest request);
    void logout(String accessToken);
}
