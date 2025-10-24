package com.dentallink.domain.user.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.user.dto.request.UserDeleteRequest;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.service.UserInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.ApiResponse.created;
import static com.dentallink.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserInternalService userInternalService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(
             @Valid @RequestBody UserSignupRequest request
    ) {
        return created(
                userInternalService.signup(request),
                "회원가입이 완료되었습니다."
        );
    }
    // 정보수정
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.updateUser(request, authUser),
                "사용자 정보가 수정되었습니다."
        );
    }
    // 비밀번호 수정
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.changePassword(request, authUser),
                "비밀번호가 성공적으로 변경되었습니다."
        );
    }
    // 회원탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Valid @RequestBody UserDeleteRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.withdraw(request, authUser),
                "회원 탈퇴 되었습니다."
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.getUser(authUser),
                "내 프로필이 조회되었습니다."
        );
    }
}
