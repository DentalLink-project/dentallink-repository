package com.dentallink.domain.user.service;

import com.dentallink.domain.auth.service.AuthService;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserInternalServiceTest {

    @InjectMocks
    private UserInternalService userInternalService;

    @Mock private UserRepository userRepository;
    @Mock private UserExternalService userExternalService;
    @Mock private AuthService authService;
    @Mock private PointAccountExternalService pointAccountExternalService;

    private User mockUser;
    private User mockAdminUser;

    @BeforeEach
    void setUp() {

        mockUser = User.of(
                "mockUser@example.com",
                "encoded-password",
                "mockUser",
                UserRole.ROLE_USER
        );
        mockAdminUser = User.of(
                "mockAdmin@example.com",
                "encoded-password",
                "mockAdmin",
                UserRole.ROLE_ADMIN
        );

        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockAdminUser, "id", 10L);
    }

    @Test
    @DisplayName("email을 통해 사용자 여부를 확인하고 True 반환")
    void existUserByEmail_true() {

        // given
        String email = "mockUser@example.com";
        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        // when
        boolean result = userInternalService.existsUserByEmail(email);

        // then
        assertTrue(result);
        verify(userRepository, times(1)).existsByEmail(email);
    }

    @Test
    @DisplayName("회원가입 진행 시 사용자가 저장 된다")
    void signup_success() {

        // given
        when(authService.passwordEncode("passwordA123!"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        UserSignupRequest request = UserSignupRequest.of(
                "mockUser",
                "mockUser@example.com",
                "passwordA123!"
        );

        // when
        UserResponse response = userInternalService.signup(request);

        // then
        assertNotNull(response.userId());
        assertEquals(mockUser.getEmail(), response.email());
        assertEquals(mockUser.getUsername(), response.username());
        assertEquals(UserRole.ROLE_USER, response.role());

        verify(userRepository).save(any(User.class));
        verify(authService).passwordEncode("passwordA123!");
    }

    @Test
    @DisplayName("내 프로필 조회 성공시 UserResponse를 응답한다")
    void getUser_success() {

        // given
        AuthUser authUser = new AuthUser(
                1L,
                "mockUser@example.com",
                UserRole.ROLE_USER
        );
        when(userExternalService.getUserById(1L))
                .thenReturn(mockUser);

        // when
        UserResponse response = userInternalService.getUser(authUser);

        // then
        assertNotNull(response.userId());
        assertEquals(mockUser.getEmail(), response.email());
        assertEquals(mockUser.getUsername(), response.username());
        assertEquals(UserRole.ROLE_USER, response.role());

        verify(userExternalService).getUserById(1L);
    }

    @Test
    @DisplayName("내 정보 수정이 성공하면 UserResponse를 응답한다")
    void updateUser_success() {

        // given
        User changeuser = User.of(
                "changeEmail@exmaple.com",
                "encoded-password",
                "username",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(changeuser, "id", 1L);
        AuthUser authUser = new AuthUser(
                1L,
                "changeEmail@example.com",
                UserRole.ROLE_USER
        );
        UserUpdateRequest request = new UserUpdateRequest(
                "mockUser",
                "mockUser@example.com",
                "passwordA123!"
        );
        when(userExternalService.getUserById(1L))
                .thenReturn(changeuser);
        doNothing().when(authService).passwordCheck("passwordA123!", changeuser.getId());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserResponse response = userInternalService.updateUser(request,authUser);

        // then
        assertNotNull(response.userId());
        assertEquals(mockUser.getEmail(), response.email());
        assertEquals(mockUser.getUsername(), response.username());
        assertEquals(UserRole.ROLE_USER, response.role());

        verify(userExternalService).getUserById(1L);
        verify(authService).passwordCheck("passwordA123!", 1L);
    }

    @Test
    @DisplayName("비밀번호 변경 성공시 UserResponse를 응답한다")
    void changePassword_success() {

        // given
        AuthUser authUser = new AuthUser(
                1L,
                "mockUser@example.com",
                UserRole.ROLE_USER
        );
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                "passwordA123!",
                "oldPassword"
        );
        when(userExternalService.getUserById(1L))
                .thenReturn(mockUser);
        doNothing().when(authService).passwordCheck("oldPassword", mockUser.getId());
        when(authService.passwordEncode("passwordA123!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        userInternalService.changePassword(request, authUser);

        // then
        verify(authService).passwordCheck("oldPassword", 1L);
        verify(authService).passwordEncode("passwordA123!");
    }
}
