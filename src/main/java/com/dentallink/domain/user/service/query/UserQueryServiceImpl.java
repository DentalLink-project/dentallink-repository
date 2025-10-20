package com.dentallink.domain.user.service.query;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.entity.User;
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
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    // ID(PK) 기준으로 유저 검색
    @Override
    public User getUserById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    // email 기준으로 유저 검색
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND)
        );
    }

    // email 기준으로 유저가 존재하는지 검색
    @Override
    public boolean existsUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 내 프로필 조회
    @Override
    public UserResponse getUser(AuthUser authUser) {
        User user = getUserById(authUser.getUserId());
        return UserResponse.from(user);
    }

    // 특정 사용자 조회
    @Override
    public UserResponse getOneUser(Long userId) {
        User user = getUserById(userId);
        return UserResponse.from(user);
    }

    // 유저 전체 조회
    @Override
    public PageResponse<UserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<User> users = userRepository.findAllByDeletedFalse(pageable);
        Page<UserResponse> response = users.map(UserResponse::from);
        return PageResponse.fromPage(response);
    }
}
