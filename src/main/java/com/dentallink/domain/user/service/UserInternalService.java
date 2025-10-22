package com.dentallink.domain.user.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.auth.service.AuthService;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.exception.UserErrorCode;
import com.dentallink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInternalService {

    private final UserRepository userRepository;
    private final AuthService authService;

    // ID(PK) 기준으로 유저 검색
    public User getUserById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    // email 기준으로 유저 검색
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    // email 기준으로 유저가 존재하는지 검색
    public boolean existsUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 특정 사용자 조회
    @Transactional(readOnly = true)
    public UserResponse getOneUser(Long userId) {
        User user = getUserById(userId);
        return UserResponse.from(user);
    }

    // 유저 전체 조회
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<User> users = userRepository.findAllByDeletedFalse(pageable);
        Page<UserResponse> response = users.map(UserResponse::from);
        return PageResponse.fromPage(response);
    }

    // 병원 권한으로 가입
    public UserResponse hospitalSignup(UserSignupRequest request) {
        if(existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                request.email(),
                authService.passwordEncode(request.password()),
                request.username(),
                UserRole.ROLE_HOSPITAL
        ));
        return UserResponse.from(user);
    }

    // 한 번만 실행 가능한 테스트용 관리자 계정 생성 메서드
    public UserResponse createTestAdmin() {
        if(existsUserByEmail("admin@example.com")) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                "admin@example.com",
                authService.passwordEncode("password123!"),
                "관리자",
                UserRole.ROLE_ADMIN
        ));
        return UserResponse.from(user);
    }
}
