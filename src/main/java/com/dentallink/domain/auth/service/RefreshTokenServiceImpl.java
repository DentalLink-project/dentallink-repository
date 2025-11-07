package com.dentallink.domain.auth.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.utility.RedisDao;
import com.dentallink.common.utility.JwtTokenProvider;
import com.dentallink.domain.auth.exception.AuthErrorCode;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.UserExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtTokenProvider  jwtTokenProvider;
    private final RedisDao redisDao;
    private final UserExternalService userExternalService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationTime;

    // refreshToken을 생성합니다.
    @Override
    public String createRefreshToken(Long userId) {
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        // Redis에 저장합니다. key : "RefreshToken:1", value : {refreshToken값}, 저장 기간 : yml 저장된 값
        redisDao.setValues(
                "RefreshToken:" + userId,
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationTime)
        );
        return refreshToken;
    }

    // RefreshToken을 읽어옵니다.
    @Override
    public String getRefreshToken(Long userId) {
        // redis 에서 객체를 읽어옵니다.
        Object token = redisDao.getValues("RefreshToken:" + userId);
        if (token == null) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        return (String) token;
    }

    // RefreshToken을 삭제합니다.
    @Override
    public void deleteRefreshToken(Long userId) {
        redisDao.deleteValues("RefreshToken:" + userId); // Key로 검색하여 삭제
    }

    // 토큰을 재발급합니다.
    @Override
    public String reissueAccessToken(String refreshToken) {

        Long userId = Long.parseLong(jwtTokenProvider.getUserInfoFromToken(refreshToken).getSubject());

        User user = userExternalService.getUserById(userId);
        String storedToken = getRefreshToken(userId);

        if (!storedToken.equals(refreshToken)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getUserRole());
    }
}
