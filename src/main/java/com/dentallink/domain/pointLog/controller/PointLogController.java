package com.dentallink.domain.pointLog.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.service.PointLogInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트 로그 관리", description = "포인트 적립/사용/환불 내역 조회 API")
@RestController
@RequestMapping("/api/point-log")
@RequiredArgsConstructor
@Validated
public class PointLogController {
    private final PointLogInternalService pointLogInternalService;

    @Operation(summary = "내 포인트 로그 조회", description = "로그인한 사용자의 포인트 내역을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PageResponse<PointLogResponse>>> getMyLogs(
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") @Min(1) int size,
            @Parameter(description = "정렬 기준 (latest or oldest)", example = "latest") @RequestParam(defaultValue = "latest") String sort
    ) {
        Long userId = authUser.getUserId();
        PageResponse<PointLogResponse> response = pointLogInternalService.getLogsByUser(userId, page, size, sort);
        return CommonApiResponse.success(response, "내 포인트 로그 조회 성공");
    }
}
