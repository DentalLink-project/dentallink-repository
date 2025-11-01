package com.dentallink.domain.auth.service;

import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.JwtToken;

public interface AuthService {

    void passwordCheck(String password, Long userId);
    String passwordEncode(String password);
    JwtToken login(LoginRequest request);
    void logout(String accessToken, String refreshToken);
    JwtToken reissueAccessToken(String refreshToken);
}
