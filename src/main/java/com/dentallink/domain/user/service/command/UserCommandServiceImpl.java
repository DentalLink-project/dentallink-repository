package com.dentallink.domain.user.service.command;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.auth.service.AuthServiceImpl;
import com.dentallink.domain.user.dto.request.UserDeleteRequest;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.exception.UserErrorCode;
import com.dentallink.domain.user.repository.UserRepository;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private final AuthServiceImpl authService;
    private final UserQueryService userQueryService;
    private final UserRepository userRepository;

    @Override
    public UserResponse signup(UserSignupRequest request) {
        if(userQueryService.existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                request.email(),
                authService.passwordEncode(request.password()),
                request.username(),
                request.userRole()
        ));
        return UserResponse.from(user);
    }

    @Override
    public UserResponse updateUser(UserUpdateRequest request, AuthUser authUser) {
        User user = userQueryService.getUserById(authUser.getUserId());
        if (request.password() == null) throw new GlobalException(UserErrorCode.USER_BAD_REQUEST);
        authService.passwordCheck(request.password(), user.getId());
        if (request.email() != null && !user.getEmail().equals(request.email()) && userQueryService.existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        user.update(request.username(), request.email());

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    @Override
    public Void changePassword(UserUpdatePasswordRequest request, AuthUser authUser) {
        User user = userQueryService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.oldPassword(), user.getId());

        user.updatePassword(authService.passwordEncode(request.newPassword()));
        userRepository.save(user);
        return null;
    }

    @Override
    public Void withdraw(UserDeleteRequest request, AuthUser authUser) {
        User user = userQueryService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.password(), user.getId());
        user.delete();
        return null;
    }
}
