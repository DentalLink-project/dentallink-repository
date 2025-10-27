package com.dentallink.domain.user.controller;

import com.dentallink.common.response.PageResponse;
import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.service.UserInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserAdminController {

    private final UserInternalService userInternalService;

    // 내부 사용자 전용(role 구분, admin만 가능한 내용)

    // 특정 유저 열람
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<CommonApiResponse<UserResponse>> getOneUser(
            @PathVariable("userId") Long userId
    ) {
        return success(
                userInternalService.getOneUser(userId),
                "회원이 조회되었습니다."
                );
    }

    // 모든 유저 열람
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<CommonApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return  success(
                userInternalService.getUsers(page, size),
                "회원 목록이 조회되었습니다."
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/hospitals/signup")
    public ResponseEntity<CommonApiResponse<UserResponse>> signup(
            @Valid @RequestBody UserSignupRequest request
    ) {
        return success(
                userInternalService.hospitalSignup(request),
                "병원 관리자 가입 완료되었습니다."
        );
    }

    @PostMapping("/admin")
    public ResponseEntity<CommonApiResponse<UserResponse>> createTestAdmin(){
        return success(
                userInternalService.createTestAdmin(),
                "테스트용 관리자 계정이 생성되었습니다."
        );
    }
}
