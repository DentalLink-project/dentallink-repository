package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentRequest;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentInternalService;
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

@Tag(name = "결제 관리", description = "포인트 충전 등 결제 관련 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {
    private final PaymentInternalService paymentInternalService;

    @Operation(
            summary = "포인트 충전 (결제)",
            description = "사용자가 결제 수단을 통해 포인트를 충전합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "포인트 충전 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (금액 또는 결제 수단 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PaymentResponse>> depositPoint(
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "결제 요청 정보") @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentInternalService.depositPoint(
                authUser.getUserId(),
                request.amount(),
                request.method()
        );
        return CommonApiResponse.created(response, "포인트 충전에 성공했습니다.");
    }
}
