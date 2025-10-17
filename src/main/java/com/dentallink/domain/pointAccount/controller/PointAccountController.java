package com.dentallink.domain.pointAccount.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.pointAccount.dto.request.PointAccountRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.service.PointAccountInternalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/points")
public class PointAccountController {
    private final PointAccountInternalService pointAccountInternalService;

    // 계좌 생성하기
    @PostMapping("/account")
    public ResponseEntity<ApiResponse<PointAccountCreateResponse>> createPointAccount() {
        PointAccountCreateResponse response = pointAccountInternalService.createPointAccount();
        return ApiResponse.created(response, "계좌 생성에 성공했습니다.");
    }

    // 현금 -> 포인트
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PointAccountDepositResponse>> chargePointAccount(
            @PathVariable Long userId, // 수정 필요
            @RequestBody PointAccountRequest request
    ){
        PointAccountDepositResponse response = pointAccountInternalService.depositPointAccount(userId, request.amount());
        return ApiResponse.success(response, "포인트 충전에 성공했습니다.");
    }

    // 포인트 -> 현금
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<PointAccountWithdrawResponse>> withdrawPointAccount(
            @PathVariable Long userId,
            @RequestBody PointAccountRequest request
    ){
        PointAccountWithdrawResponse response = pointAccountInternalService.withdrawPointAccount(userId, request.amount());
        return ApiResponse.success(response, "포인트를 현금으로 전환했습니다.");
    }

    // 포인트 -> 상품 구매
    @PostMapping("/use")
    public ResponseEntity<ApiResponse<PointAccountSpendResponse>> spendPointAccount(
            @PathVariable Long userId, // 수정 필요
            @RequestBody PointAccountRequest request
    ){
        PointAccountSpendResponse response = pointAccountInternalService.spendPointAccount(userId, request.amount());
        return ApiResponse.success(response, "포인트를 사용하였습니다.");
    }

    // 포인트 환불하기
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PointAccountRefundResponse>> refundPointAccount(
            @PathVariable Long userId, // 수정 필요,
            @RequestBody PointAccountRequest request
    ){
        PointAccountRefundResponse response = pointAccountInternalService.refundPointAccount(userId, request.amount());
        return ApiResponse.success(response, "환불에 성공했습니다.");
    }

    // 잔액 확인하기
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<PointAccountGetResponse>> getPointAccount(
            @PathVariable Long userId // 수정 필요,
    ){
        PointAccountGetResponse response = pointAccountInternalService.getPointAccount(userId);
        return ApiResponse.success(response, "잔액을 확인하였습니다.");
    }
}
