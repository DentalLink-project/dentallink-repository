package com.dentallink.domain.user.service;

import com.dentallink.domain.auth.service.AuthService;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
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

    @Mock
    private UserRepository userRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private UserInternalService userInternalService;
    private User mockUser;
    private User mockAdminUser;
    private String mockPlanePassword;

    @BeforeEach
    void setUp() {
        mockPlanePassword = "passwordA123!";

        when(authService.passwordEncode(mockPlanePassword))
                .thenReturn("encoded-password");

        mockUser = User.of(
                "mockUser@example.com",
                authService.passwordEncode(mockPlanePassword),
                "mockUser",
                UserRole.ROLE_USER
        );
        mockAdminUser = User.of(
                "mockAdmin@example.com",
                authService.passwordEncode(mockPlanePassword),
                "mockAdmin",
                UserRole.ROLE_ADMIN
        );
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockAdminUser, "id", 10L);
    }

    @Test
    @DisplayName("email을 통해 유저가 존재하는지 확인하고, 존재하여 True 반환")
    void existsUserByEmail_true() {
        // given
        when(userRepository.existsByEmail(mockUser.getEmail()))
                .thenReturn(true);
        // when
        boolean isExists = userInternalService.existsUserByEmail(mockUser.getEmail());
        // then
        assertTrue(isExists);
        verify(userRepository.existsByEmail(mockUser.getEmail()));
    }

    @Test
    @DisplayName("email을 통해 유저가 존재하는지 확인하고, 존재하지 않아 False 반환")
    void existsUserByEmail_false() {
        // given
        when(userRepository.existsByEmail(mockUser.getEmail()))
                .thenReturn(false);
        // when
        boolean isExists = userInternalService.existsUserByEmail(mockUser.getEmail());
        // then
        assertFalse(isExists);
        verify(userRepository).existsByEmail(mockUser.getEmail());
    }

    @Test
    @DisplayName("회원가입 성공 시 유저가 저장된다")
    void createUser_success() {
        // given
        UserSignupRequest request = UserSignupRequest.of(
                mockUser.getUsername(),
                mockUser.getEmail(),
                mockPlanePassword
        );
        when(authService.passwordEncode(request.password()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    ReflectionTestUtils.setField(user, "id", 1L); // id 자동 생성 흉내
                    return user;
                });

        // when
        UserResponse savedUserResponse = userInternalService.signup(request);

        assertNotNull(savedUserResponse.userId());
        assertEquals(mockUser.getEmail(), savedUserResponse.email());
        assertEquals(mockUser.getUsername(), savedUserResponse.username());
        assertEquals(UserRole.ROLE_USER, savedUserResponse.role());

        verify(userRepository).save(any(User.class));
        verify(authService).passwordEncode(mockPlanePassword);
    }


}
