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

    private final UserRepository userRepository;

    // ID를 기준으로 사용자 조회
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }
    // email 기준으로 유저 검색
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND)
        );
    }
}
