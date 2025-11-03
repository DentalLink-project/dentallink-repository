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
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "결제 관리 테스트", description = "Toss 결제 및 포인트 충전 API (테스트용)")
@RestController
@RequestMapping("/api/test/payments")
@RequiredArgsConstructor
@Validated
public class PaymentTestController {

    private final PaymentTestService paymentTestService;

    // 1. 결제 준비 (READY 생성)
    @Operation(summary = "테스트 결제 준비", description = "READY 상태의 테스트 결제를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 준비 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
    })
    @PostMapping("/ready")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentReadyResponse>> testReadyPayment(
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentReadyRequest request
    ) {
        Long userId = (authUser != null) ? authUser.getUserId() : 17L;
        PaymentReadyResponse response = paymentTestService.testReadyPayment(userId, request);
        return CommonApiResponse.created(response, "테스트 결제 준비가 완료되었습니다.");
    }

    // 2. 결제 취소 (READY 상태만)
    @Operation(summary = "테스트 결제 취소", description = "READY 상태의 결제를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 취소 완료"),
            @ApiResponse(responseCode = "400", description = "취소할 수 없는 결제 상태입니다.")
    })
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentCancelResponse>> testCancelPayment(
            @Parameter(description = "주문 ID", example = "ORDER-12345") @PathVariable String orderId
    ) {
        PaymentCancelResponse response = paymentTestService.testCancelPayment(orderId);
        return CommonApiResponse.success(response, "테스트 결제가 정상적으로 취소되었습니다.");
    }

    // 3. 결제 승인 (SUCCESS 처리)
    @Operation(summary = "테스트 결제 승인", description = "Toss 결제 성공 후 서버 승인 처리를 테스트합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 승인 완료"),
            @ApiResponse(responseCode = "400", description = "결제 승인 중 오류 발생")
    })
    @PostMapping("/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentResponse>> testConfirmPayment(
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        Long userId = authUser.getUserId();
        PaymentResponse response = paymentTestService.testConfirmPayment(userId, request);
        return CommonApiResponse.created(response, "테스트 결제가 승인되었습니다.");
    }
}
