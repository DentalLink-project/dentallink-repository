package com.dentallink.domain.user.controller;

import com.dentallink.common.response.PageResponse;
import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.service.command.UserCommandService;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserInternalController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    // 내부 사용자 전용(role 구분, admin만 가능한 내용)

    // Query Service
    // 특정 유저 열람
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getOneUser(
            @PathVariable("userId") Long userId
    ) {
        return success(
                userQueryService.getOneUser(userId),
                "회원이 조회되었습니다."
                );
    }

    // 모든 유저 열람
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return  success(
                userQueryService.getUsers(page, size),
                "회원 목록이 조회되었습니다."
        );
    }
}
