package com.dentallink.domain.user.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.user.dto.request.UserDeleteRequest;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.request.UserUpdatePasswordRequest;
import com.dentallink.domain.user.dto.request.UserUpdateRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.service.UserInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.created;
import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "회원 관리", description = "일반 사용자를 위한 회원 관리 API")
public class UserController {

    private final UserInternalService userInternalService;

    // 회원가입
    @Operation(summary = "회원 가입",
            description = "회원가입을 위한 메서드입니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "생성 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PostMapping("/signup")
    public ResponseEntity<CommonApiResponse<UserResponse>> signup(
             @Parameter(description = "가입을 위한 회원 정보")
             @Valid @RequestBody UserSignupRequest request
    ) {
        return created(
                userInternalService.signup(request),
                "회원가입이 완료되었습니다."
        );
    }

    // 정보수정
    @Operation(summary = "사용자 정보 수정",
            description = "로그인한 사용자 본인의 정보를 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PatchMapping("/me")
    public ResponseEntity<CommonApiResponse<UserResponse>> updateUser(
            @Parameter(description = "수정될 회원 정보 및 기존 암호")
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.updateUser(request, authUser),
                "사용자 정보가 수정되었습니다."
        );
    }
    // 비밀번호 수정
    @Operation(summary = "사용자 암호 수정",
            description = "로그인한 사용자 본인의 암호를 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PutMapping("/password")
    public ResponseEntity<CommonApiResponse<Void>> changePassword(
            @Parameter(description = "새 암호 및 기존 암호")
            @Valid @RequestBody UserUpdatePasswordRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.changePassword(request, authUser),
                "비밀번호가 성공적으로 변경되었습니다."
        );
    }
    // 회원탈퇴
    @Operation(summary = "사용자 회원 탈퇴",
            description = "로그인한 사용자 회원 탈퇴.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @DeleteMapping("/withdraw")
    public ResponseEntity<CommonApiResponse<Void>> withdraw(
            @Parameter(description = "사용중인 암호")
            @Valid @RequestBody UserDeleteRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.withdraw(request, authUser),
                "회원 탈퇴 되었습니다."
        );
    }

    // 내 정보 조회
    @Operation(summary = "사용자 정보 조회",
            description = "로그인한 사용자 정보 조회.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @GetMapping("/me")
    public ResponseEntity<CommonApiResponse<UserResponse>> getUser(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                userInternalService.getUser(authUser),
                "내 프로필이 조회되었습니다."
        );
    }
}
