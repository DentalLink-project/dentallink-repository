package com.dentallink.domain.pointAccount.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.pointAccount.dto.request.PointAccountDepositRequest;
import com.dentallink.domain.pointAccount.dto.request.PointAccountWithdrawRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.service.PointAccountInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트 계좌 관리", description = "포인트 계좌 및 잔액 관련 API")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Validated
public class PointAccountController {
    private final PointAccountInternalService pointAccountInternalService;

    @Operation(summary = "포인트 계좌 잔액 조회", description = "사용자의 포인트 계좌 잔액을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PointAccountGetResponse>> getPointAccount(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PointAccountGetResponse response = pointAccountInternalService.getPointAccount(authUser.getUserId());
        return CommonApiResponse.success(response, "잔액을 확인하였습니다.");
    }

    @Operation(summary = "포인트 충전", description = "관리자가 특정 포인트 계좌에 포인트를 충전합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "충전 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음") // 확인 필요
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deposit")
    public ResponseEntity<CommonApiResponse<PointAccountDepositResponse>> depositPointAccount(
            @Valid @RequestBody PointAccountDepositRequest request
    ) {
        PointAccountDepositResponse response = pointAccountInternalService.depositPointAccount(
                request.accountId(),
                request.amount()
        );
        return CommonApiResponse.success(response, "포인트 충전에 성공했습니다.");
    }


    @Operation(summary = "포인트 출금", description = "사용자가 보유 포인트를 본인 계좌로 출금합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출금 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/withdraw")
    public ResponseEntity<CommonApiResponse<PointAccountWithdrawResponse>> withdrawPointAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PointAccountWithdrawRequest request
    ) {
        PointAccountWithdrawResponse response =
                pointAccountInternalService.withdrawPointAccount(authUser.getUserId(), request);

        return CommonApiResponse.success(response, "포인트 출금 요청이 접수되었습니다.");
    }
}

