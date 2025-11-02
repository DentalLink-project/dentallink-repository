package com.dentallink.domain.auth.service;

public interface RefreshTokenService {
    String createRefreshToken(Long userId);
    String getRefreshToken(Long userId);
    void deleteRefreshToken(Long userId);
    String reissueAccessToken(String refreshToken);
}
