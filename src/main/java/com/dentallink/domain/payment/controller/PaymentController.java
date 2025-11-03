package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentReadyRequest;
import com.dentallink.domain.payment.dto.request.PaymentConfirmRequest;
import com.dentallink.domain.payment.dto.response.PaymentCancelResponse;
import com.dentallink.domain.payment.dto.response.PaymentReadyResponse;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 관리", description = "Toss 결제 및 포인트 충전 API (실서비스용)")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비
    @Operation(summary = "결제 준비", description = "결제 요청 정보를 서버에 미리 저장합니다." )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 준비 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (요청 데이터 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/ready")
    public ResponseEntity<CommonApiResponse<PaymentReadyResponse>> saveReadyPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentReadyRequest request
    ) {
        PaymentReadyResponse response = paymentService.saveReadyPayment(authUser.getUserId(), request);
        return CommonApiResponse.created(response, "결제 준비가 완료되었습니다.");
    }

    // 결제 승인
    @Operation(summary = "결제 승인", description = "Toss 서버에 결제 승인 요청을 보내고, 결제를 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "포인트 충전 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (결제 정보 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/confirm")
    public ResponseEntity<CommonApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentResponse response = paymentService.confirmPayment(authUser.getUserId(), request);
        return CommonApiResponse.created(response, "포인트 충전이 완료되었습니다.");
    }

    // READY 상태 결제 취소
    @Operation(summary = "READY 상태 결제 취소", description = "아직 승인되지 않은 결제(READY 상태)를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 취소 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (결제 상태 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/cancel/ready/{orderId}")
    public ResponseEntity<CommonApiResponse<PaymentCancelResponse>> cancelReadyPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String orderId
    ) {
        PaymentCancelResponse response = paymentService.cancelReadyPayment(authUser.getUserId(), orderId);
        return CommonApiResponse.success(response, "결제가 정상적으로 취소되었습니다.");
    }
}
