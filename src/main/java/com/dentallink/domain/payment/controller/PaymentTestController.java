package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentConfirmRequest;
import com.dentallink.domain.payment.dto.request.PaymentReadyRequest;
import com.dentallink.domain.payment.dto.response.PaymentCancelResponse;
import com.dentallink.domain.payment.dto.response.PaymentReadyResponse;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentTestService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 관리 테스트", description = "Toss 결제 및 포인트 충전 API 테스트용")
@RestController
@RequestMapping("/api/test/payments")
@RequiredArgsConstructor
@Validated
public class PaymentTestController {
    private final PaymentTestService paymentTestService;

    // 1. 결제 준비 (READY 생성)
    @Operation(summary = "테스트 결제 준비", description = "Toss 결제 전 단계로, READY 상태의 결제를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 준비 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
    })

    @PostMapping("/ready")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentReadyResponse>> testReadyPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody PaymentReadyRequest request
    ) {
        PaymentReadyResponse response = paymentTestService.testReadyPayment(authUser.getUserId(), request);
        return CommonApiResponse.created(response, "결제 준비가 완료되었습니다.");
    }

    // 2. ready 취소
    // 2. 결제 취소 (READY 상태만 가능)
    @Operation(summary = "테스트 결제 취소", description = "READY 상태의 결제를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 취소 완료"),
            @ApiResponse(responseCode = "400", description = "취소할 수 없는 결제 상태입니다.")
    })
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentCancelResponse>> testCancelPayment(
            @PathVariable String orderId
    ) {
        PaymentCancelResponse response = paymentTestService.testCancelPayment(orderId);
        return CommonApiResponse.created(response, "결제가 성공적으로 취소되었습니다.");
    }

    // 3. 결제 승인 (결제 성공 처리)
    @Operation(summary = "테스트 결제 승인", description = "Toss 결제 성공 후 서버 승인 처리를 진행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 완료"),
            @ApiResponse(responseCode = "400", description = "결제 승인 중 오류 발생")
    })
    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentResponse>> testConfirmPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentResponse response = paymentTestService.testConfirmPayment(authUser.getUserId(), request);
        return CommonApiResponse.created(response, "포인트 충전이 완료되었습니다.");
    }
}
