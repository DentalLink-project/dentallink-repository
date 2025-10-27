package com.dentallink.domain.user.controller;

import com.dentallink.common.response.PageResponse;
import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.user.dto.request.UserSignupRequest;
import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.service.UserInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.created;
import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "ADMIN 회원 관리", description = "ADMIN 권한과 관련된 회원 관리 API")
public class UserAdminController {

    private final UserInternalService userInternalService;

    // ADMIN 전용 컨트롤러입니다.

    // 특정 유저 열람
    @Operation(summary = "회원 조회",
            description = "특정 회원을 조회합니다.",
            security = {@SecurityRequirement(name = "sessionAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<CommonApiResponse<UserResponse>> getOneUser(
            @Parameter(description = "조회할 사용자 PK") @PathVariable("userId") Long userId
    ) {
        return success(
                userInternalService.getOneUser(userId),
                "회원이 조회되었습니다."
                );
    }

    // 모든 유저 열람
    @Operation(summary = "전체 회원 조회",
            description = "모든 회원을 조회합니다.",
            security = {@SecurityRequirement(name = "sessionAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<CommonApiResponse<PageResponse<UserResponse>>> getUsers(
            @Parameter(description = "페이지") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        return  success(
                userInternalService.getUsers(page, size),
                "회원 목록이 조회되었습니다."
        );
    }

    @Operation(summary = "테스트용 ADMIN 생성",
            description = "테스트를 위한 ADMIN 계정을 한 번 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "생성 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PostMapping("/admin")
    public ResponseEntity<CommonApiResponse<UserResponse>> createTestAdmin(){
        return created(
                userInternalService.createTestAdmin(),
                "테스트용 관리자 계정이 생성되었습니다."
        );
    }

    // 병원 관계자 가입
    @Operation(summary = "병원 관계자 가입",
            description = "ADMIN 권한으로 병원 관계자 자격의 사용자를 생성합니다.",
            security = {@SecurityRequirement(name = "sessionAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/hospitals/signup")
    public ResponseEntity<CommonApiResponse<UserResponse>> signup(
            @Parameter(description = "사용자 가입을 위한 정보")@Valid @RequestBody UserSignupRequest request
    ) {
        return created(
                userInternalService.hospitalSignup(request),
                "병원 관리자 가입 완료되었습니다."
        );
    }
}
