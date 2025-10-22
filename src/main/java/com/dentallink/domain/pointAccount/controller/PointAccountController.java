package com.dentallink.domain.pointAccount.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.pointAccount.dto.request.PointAccountRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.service.PointAccountInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/points")
public class PointAccountController {
    private final PointAccountInternalService pointAccountInternalService;

    // 계좌 생성하기
    @PostMapping("/account")
    public ResponseEntity<ApiResponse<PointAccountCreateResponse>> createPointAccount(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PointAccountCreateResponse response = pointAccountInternalService.createPointAccount(authUser.getUserId());
        return ApiResponse.created(response, "계좌 생성에 성공했습니다.");
    }

    // 잔액 확인하기
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<PointAccountGetResponse>> getPointAccount(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PointAccountGetResponse response = pointAccountInternalService.getPointAccount(authUser.getUserId());
        return ApiResponse.success(response, "잔액을 확인하였습니다.");
    }

    // 관계자가 특정 포인트 계좌에 포인트를 충전하는 메서드
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PointAccountDepositResponse>> chargePointAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PointAccountRequest request
    ){
        PointAccountDepositResponse response = pointAccountInternalService.depositPointAccount(authUser.getUserId(), request.amount());
        return ApiResponse.success(response, "포인트 충전에 성공했습니다.");
    }

    // 포인트를 현금으로 전환(지금은 그냥 point의 balance 값을 줄이는 용도)
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<PointAccountWithdrawResponse>> withdrawPointAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PointAccountRequest request
    ){
        PointAccountWithdrawResponse response = pointAccountInternalService.withdrawPointAccount(authUser.getUserId(), request.amount());
        return ApiResponse.success(response, "포인트를 현금으로 전환했습니다.");
    }

}

