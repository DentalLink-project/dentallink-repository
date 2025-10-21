package com.dentallink.domain.pointLog.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.service.PointLogInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/point-log")
public class PointLogController {
    private final PointLogInternalService pointLogInternalService;

    @GetMapping("/logs/me")
    public ResponseEntity<ApiResponse<PageResponse<PointLogResponse>>> getMyLogs(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long userId = authUser.getUserId();
        PageResponse<PointLogResponse> response = pointLogInternalService.getLogsByUser(userId, page, size, sort);
        return ApiResponse.success(response, "내 포인트 로그 조회 성공");
    }
}
