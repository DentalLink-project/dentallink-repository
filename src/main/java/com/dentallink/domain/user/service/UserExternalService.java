package com.dentallink.domain.user.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.auth.service.AuthService;
import com.dentallink.domain.user.dto.request.UserDeleteRequest;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.exception.UserErrorCode;
import com.dentallink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserExternalService {

    private final UserInternalService userInternalService;
    private final AuthService authService;
    private final UserRepository userRepository;

    // ID를 기준으로 사용자 조회
    @Transactional(readOnly = true)
    public User getUserById(Long Id) {
        return userInternalService.getUserById(Id);
    }

    // 내 프로필 조회
    @Transactional(readOnly = true)
    public UserResponse getUser(AuthUser authUser) {
        User user = userInternalService.getUserById(authUser.getUserId());
        return UserResponse.from(user);
    }

    public UserResponse signup(UserSignupRequest request) {
        if(userInternalService.existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                request.email(),
                authService.passwordEncode(request.password()),
                request.username(),
                UserRole.ROLE_USER
        ));
        return UserResponse.from(user);
    }

    public UserResponse updateUser(UserUpdateRequest request, AuthUser authUser) {
        User user = userInternalService.getUserById(authUser.getUserId());
        if (request.password() == null) throw new GlobalException(UserErrorCode.USER_BAD_REQUEST);
        authService.passwordCheck(request.password(), user.getId());
        if (request.email() != null && !user.getEmail().equals(request.email()) && userInternalService.existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        user.update(request.username(), request.email());

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public Void changePassword(UserUpdatePasswordRequest request, AuthUser authUser) {
        User user = userInternalService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.oldPassword(), user.getId());

        user.updatePassword(authService.passwordEncode(request.newPassword()));
        userRepository.save(user);
        return null;
    }

    public Void withdraw(UserDeleteRequest request, AuthUser authUser) {
        User user = userInternalService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.password(), user.getId());
        user.delete();
        return null;
    }
}
