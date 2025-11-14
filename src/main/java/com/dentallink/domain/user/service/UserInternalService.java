package com.dentallink.domain.user.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.auth.service.AuthService;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInternalService {

    private final UserExternalService userExternalService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PointAccountExternalService  pointAccountExternalService;

    // ---------- 내부 사용 검색 기능 ----------

    // email 기준으로 유저가 존재하는지 검색
    @Transactional(readOnly = true)
    public boolean existsUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ---------- 일반 사용자 기능 ----------

    // 회원가입
    public UserResponse signup(UserSignupRequest request) {
        if(existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                request.email(),
                authService.passwordEncode(request.password()),
                request.username(),
                UserRole.ROLE_USER
        ));
        pointAccountExternalService.createPointAccount(user);
        return UserResponse.from(user);
    }

    // 내 프로필 조회
    @Transactional(readOnly = true)
    public UserResponse getUser(AuthUser authUser) {
        User user = userExternalService.getUserById(authUser.getUserId());
        return UserResponse.from(user);
    }

    // 내 사용자 정보 수정
    public UserResponse updateUser(UserUpdateRequest request, AuthUser authUser) {
        User user = userExternalService.getUserById(authUser.getUserId());
        if (request.password() == null) throw new GlobalException(UserErrorCode.USER_BAD_REQUEST);
        authService.passwordCheck(request.password(), user.getId());
        if (request.email() != null && !user.getEmail().equals(request.email()) && existsUserByEmail(request.email())) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        user.update(request.username(), request.email());

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    // 내 비밀번호 변경
    public Void changePassword(UserUpdatePasswordRequest request, AuthUser authUser) {
        User user = userExternalService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.oldPassword(), user.getId());

        user.updatePassword(authService.passwordEncode(request.newPassword()));
        userRepository.save(user);
        return null;
    }

    // 회원 탈퇴
    public Void withdraw(UserDeleteRequest request, AuthUser authUser) {
        User user = userExternalService.getUserById(authUser.getUserId());
        authService.passwordCheck(request.password(), user.getId());
        user.delete();
        userRepository.save(user);
        return null;
    }

    // ---------- 테스트를 위한 관리자 계정 생성 ----------
    public UserResponse createTestAdmin() {
        if(existsUserByEmail("admin@example.com")) {
            throw new GlobalException(UserErrorCode.EMAIL_DUPLICATED);
        }
        User user = userRepository.save(User.of(
                "admin@example.com",
                authService.passwordEncode("passwordA123!"),
                "관리자",
                UserRole.ROLE_ADMIN
        ));
        pointAccountExternalService.createPointAccount(user);
        return UserResponse.from(user);
    }

    // ---------- ADMIN 기능 ----------//
    // 특정 사용자 조회
    @Transactional(readOnly = true)
    public UserResponse getOneUser(Long userId) {
        User user = userExternalService.getUserById(userId);
        return UserResponse.from(user);
    }

    // 유저 전체 조회
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size, String role) {

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<User> users;

        if (role.equals("none")) {
            users = userRepository.findAllByDeletedFalse(pageable);
        } else {
            try {
                users = userRepository
                        .findAllByUserRoleAndDeletedFalse(
                                pageable,
                                UserRole.valueOf("ROLE_" + role.toUpperCase())
                        );
            } catch (IllegalArgumentException e) {
                throw new GlobalException(UserErrorCode.INVALID_ROLE);
            }
        }

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
        pointAccountExternalService.createPointAccount(user);
        return UserResponse.from(user);
    }
}
