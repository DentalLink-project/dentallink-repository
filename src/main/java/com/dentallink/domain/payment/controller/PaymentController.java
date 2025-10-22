package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentRequest;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentInternalService paymentInternalService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PaymentResponse>> depositPoint(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentInternalService.depositPoint(
                authUser.getUserId(),        // 로그인된 사용자 ID
                request.amount(),            // 요청 금액
                request.method()             // 결제 수단
        );

        return ApiResponse.created(response, "포인트 충전에 성공했습니다.");
    }
}
