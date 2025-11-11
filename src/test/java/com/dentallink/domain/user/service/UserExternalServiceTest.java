package com.dentallink.domain.user.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.auth.service.AuthService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserExternalServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private UserExternalService userExternalService;
    private User mockUser;

    @BeforeEach
    void setUp() {
        when(authService.passwordEncode("passwordA123!"))
                .thenReturn("encoded-password");

         mockUser = User.of(
                "mockUser@example.com",
                authService.passwordEncode("passwordA123!"),
                "mockUser",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(mockUser, "id", 1L);
    }

    @Test
    @DisplayName("email 주소를 통해 유저를 검색 성공한다.")
    void getUserByEmail_success() {

        // given
        when(userRepository.findByEmailAndDeletedAtIsNull(mockUser.getEmail()))
                .thenReturn(Optional.of(mockUser));

        // when
        User foundUser = userExternalService.getUserByEmail(mockUser.getEmail());

        // then
        assertNotNull(foundUser);
        assertEquals(mockUser.getEmail(), foundUser.getEmail());
        verify(userRepository).findByEmailAndDeletedAtIsNull(mockUser.getEmail());
    }

    @Test
    @DisplayName("email 주소를 통해 없거나 삭제된 유저를 검색할 경우 예외를 던진다.")
    void getUserByEmail_notFound_throw() {

        // given
        String email = "notfound@example.com";
        when(userRepository.findByEmailAndDeletedAtIsNull(email))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                GlobalException.class, // 발생해야 할 예외 클래스
                () -> userExternalService.getUserByEmail(email)
        );

        // verify
        verify(userRepository).findByEmailAndDeletedAtIsNull(email);
    }

    @Test
    @DisplayName("userId를 통해 유저를 검색 성공한다.")
    void getUserById_success() {
        // given
        when(userRepository.findByIdAndDeletedAtIsFalse(mockUser.getId()))
                .thenReturn(Optional.of(mockUser));

        // when
        User foundUser = userExternalService.getUserById(mockUser.getId());

        // then
        assertNotNull(foundUser);
        assertEquals(mockUser.getId(), foundUser.getId());
        verify(userRepository).findByIdAndDeletedAtIsFalse(mockUser.getId());
    }

    @Test
    @DisplayName("userId 를 통해 없거나 삭제된 유저를 검색할 경우 예외를 던진다.")
    void getUserById_notFound_throw() {
        // given
        long notExistId = 100L;
        when(userRepository.findByIdAndDeletedAtIsFalse(100L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                GlobalException.class, // 발생해야 할 예외 클래스
                () -> userExternalService.getUserById(notExistId)
        );

        // verify
        verify(userRepository).findByIdAndDeletedAtIsFalse(notExistId);
    }
}
