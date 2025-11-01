package com.dentallink.domain.auth.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.utility.JwtTokenProvider;
import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.JwtToken;
import com.dentallink.domain.auth.exception.AuthErrorCode;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.UserExternalService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService{

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final UserExternalService userExternalService;
    private final RefreshTokenService  refreshTokenService;

    // 비밀번호를 확인하는 메서드입니다.
    @Override
    public void passwordCheck(String password, Long userId) {
        if (!passwordEncoder.matches(password, userExternalService.getUserById(userId).getPassword())) {
            throw new GlobalException(AuthErrorCode.LOGIN_FAILED);
        }
    }
    // 비밀번호 암호화 메서드입니다.
    public String passwordEncode(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public JwtToken login(LoginRequest request) {

        User user = userExternalService.getUserByEmail(request.email());
        passwordCheck(request.password(), user.getId());

        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getUserRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return JwtToken.of(token, refreshToken);
    }

    public JwtToken reissueAccessToken(String refreshToken) {
        return JwtToken.of(
                refreshTokenService.reissueAccessToken(refreshToken),
                refreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        try {
            Claims claimRefreshToken = jwtTokenProvider.getUserInfoFromToken(refreshToken);
            // refreshToken 먼저 삭제
            refreshTokenService.deleteRefreshToken(Long.parseLong(claimRefreshToken.getSubject()));

            Claims claimAccessToken = jwtTokenProvider.getUserInfoFromToken(accessToken);
            Date expiration = claimAccessToken.getExpiration();
            long now = new Date().getTime();
            long remainingTime = expiration.getTime() - now;

            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(accessToken, "logout", remainingTime, TimeUnit.MILLISECONDS);
            }
        } catch (ExpiredJwtException e) {
            // 만료되었을 경우, 더이상의 작업은 필요하지 않습니다.
        }
    }

}
