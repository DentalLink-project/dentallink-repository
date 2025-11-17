package com.dentallink.domain.auth.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.utility.JwtTokenProvider;
import com.dentallink.common.utility.RedisDao;
import com.dentallink.domain.auth.exception.AuthErrorCode;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.service.UserExternalService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

public class RefreshTokenServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisDao redisDao;

    @Mock
    private UserExternalService userExternalService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User mockUser;
    private final Long mockUserId = 1L;
    private final String refreshToken = "refresh.token.value";
    private final String accessToken = "access.token.value";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationTime", 100000L);

        mockUser = User.of(
                "test@example.com",
                "encodedPassword",
                "testUser",
                UserRole.ROLE_USER
        );

        // id 필드가 setter 없이 private이면 ReflectionTestUtils로 강제 주입
        ReflectionTestUtils.setField(mockUser, "id", mockUserId);
    }

    @Test
    @DisplayName("refreshToken 생성 성공 시 Redis에 저장 후 토큰 반환")
    void createRefreshToken_success() {
        given(jwtTokenProvider.createRefreshToken(mockUserId)).willReturn(refreshToken);
        willDoNothing().given(redisDao).setValues(
                eq("RefreshToken:" + mockUserId),
                eq(refreshToken),
                any(Duration.class)
        );

        String result = refreshTokenService.createRefreshToken(mockUserId);

        assertThat(result).isEqualTo(refreshToken);
        then(redisDao).should().setValues(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("refreshToken 조회 성공 시 Redis 값 반환")
    void getRefreshToken_success() {
        given(redisDao.getValues("RefreshToken:" + mockUserId)).willReturn(refreshToken);

        String result = refreshTokenService.getRefreshToken(mockUserId);

        assertThat(result).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("refreshToken 조회 실패 시 예외 발생")
    void getRefreshToken_fail() {
        given(redisDao.getValues("RefreshToken:" + mockUserId)).willReturn(null);

        assertThatThrownBy(() -> refreshTokenService.getRefreshToken(mockUserId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("refreshToken 삭제 성공")
    void deleteRefreshToken_success() {
        willDoNothing().given(redisDao).deleteValues("RefreshToken:" + mockUserId);

        refreshTokenService.deleteRefreshToken(mockUserId);

        then(redisDao).should().deleteValues("RefreshToken:" + mockUserId);
    }

    @Test
    @DisplayName("accessToken 재발급 성공")
    void reissueAccessToken_success() {
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn(String.valueOf(mockUserId));
        given(jwtTokenProvider.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
        given(userExternalService.getUserById(mockUserId)).willReturn(mockUser);
        given(redisDao.getValues("RefreshToken:" + mockUserId)).willReturn(refreshToken);
        given(jwtTokenProvider.createAccessToken(mockUserId, mockUser.getEmail(), mockUser.getUserRole()))
                .willReturn(accessToken);

        String result = refreshTokenService.reissueAccessToken(refreshToken);

        assertThat(result).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("저장된 refreshToken과 불일치 시 예외 발생")
    void reissueAccessToken_invalidToken() {
        Claims mockClaims = mock(Claims.class);
        given(mockClaims.getSubject()).willReturn(String.valueOf(mockUserId));
        given(jwtTokenProvider.getUserInfoFromToken(refreshToken)).willReturn(mockClaims);
        given(userExternalService.getUserById(mockUserId)).willReturn(mockUser);
        given(redisDao.getValues("RefreshToken:" + mockUserId)).willReturn("different.token");

        assertThatThrownBy(() -> refreshTokenService.reissueAccessToken(refreshToken))
                .isInstanceOf(GlobalException.class)
                .hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }
}
