package com.dentallink.domain.user.service.query;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.entity.User;

public interface UserQueryService {

    User getUserById(Long id);
    User getUserByEmail(String email);
    boolean existsUserByEmail(String email);

    UserResponse getUser(AuthUser authUser);

    UserResponse getOneUser(Long userId);
    PageResponse<UserResponse> getUsers(int page, int size);
}
