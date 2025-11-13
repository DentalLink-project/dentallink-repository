package com.dentallink.domain.auth.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.utility.JwtTokenProvider;
import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.JwtToken;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.service.UserExternalService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private UserExternalService userExternalService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.of(
                "test@example.com",
                "encodedPassword",
                "testUser",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(mockUser, "id", 1L);
    }

    @Test
    @DisplayName("비밀번호가 일치하면 예외 없이 통과한다")
    void passwordCheck_success() {

        // given
        given(userExternalService.getUserById(1L)).willReturn(mockUser);
        given(passwordEncoder.matches("rawPassword", "encodedPassword")).willReturn(true);

        // when & then
        assertDoesNotThrow(() -> authService.passwordCheck("rawPassword", 1L));
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외 발생")
    void passwordCheck_fail() {

        // given
        given(userExternalService.getUserById(1L)).willReturn(mockUser);
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThrows(GlobalException.class, () -> authService.passwordCheck("wrongPassword", 1L));
    }

    @Test
    @DisplayName("비밀번호 암호화 성공")
    void passwordEncode_success() {

        // given
        given(passwordEncoder.encode("passwordA123!")).willReturn("encodedPassword");

        // when
        String result = authService.passwordEncode("passwordA123!");

        // then
        assertEquals("encodedPassword", result);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 반환한다")
    void login_success() {

        // given
        LoginRequest request = new LoginRequest("test@example.com", "passwordA123!");
        given(userExternalService.getUserByEmail("test@example.com")).willReturn(mockUser);
        given(passwordEncoder.matches("passwordA123!", "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), any())).willReturn("accessToken");
        given(refreshTokenService.createRefreshToken(1L)).willReturn("refreshToken");
        given(userExternalService.getUserById(1L)).willReturn(mockUser);

        // when
        JwtToken token = authService.login(request);

        // then
        assertNotNull(token);
        assertEquals("accessToken", token.getAccessToken());
        assertEquals("refreshToken", token.getRefreshToken());
    }

    @Test
    @DisplayName("로그아웃 시 refreshToken 삭제 및 accessToken 블랙리스트 등록")
    void logout_success() {

        // given
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        Claims accessClaims = mock(Claims.class);
        Claims refreshClaims = mock(Claims.class);

        given(jwtTokenProvider.getUserInfoFromToken(refreshToken)).willReturn(refreshClaims);
        given(jwtTokenProvider.getUserInfoFromToken(accessToken)).willReturn(accessClaims);
        given(refreshClaims.getSubject()).willReturn("1");
        given(accessClaims.getExpiration()).willReturn(new Date(System.currentTimeMillis() + 60000)); // 1분 남음
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.logout(accessToken, refreshToken);

        // then
        verify(refreshTokenService).deleteRefreshToken(1L);
        verify(valueOperations).set(eq(accessToken), eq("logout"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}
