package com.dentallink.domain.auth.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.utility.JwtUtil;
import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.TokenResponse;
import com.dentallink.domain.auth.exception.AuthErrorCode;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.query.UserQueryService;
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

    private final UserQueryService userQueryService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    // 비밀번호를 확인하는 메서드입니다.
    @Override
    public void passwordCheck(String password, Long userId) {
        if (userQueryService.getUserById(userId).getPassword().equals(passwordEncoder.encode(password))) {
            throw new GlobalException(AuthErrorCode.LOGIN_FAILED);
        }
    }
    // 비밀번호 암호화 메서드입니다.
    public String passwordEncode(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userQueryService.getUserByEmail(request.email());
        passwordCheck(request.password(), user.getId());
        String token = jwtUtil.createToken(user.getId(), user.getEmail(), user.getUserRole());
        return TokenResponse.of(token);
    }

    @Override
    public void logout(String accessToken) {
        try {
            Claims claims = jwtUtil.getUserInfoFromToken(accessToken);

            Date expiration = claims.getExpiration();
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
