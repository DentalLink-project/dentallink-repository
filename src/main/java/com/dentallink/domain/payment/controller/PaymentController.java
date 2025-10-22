package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentRequest;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
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
            @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentInternalService.depositPoint(authUser.getUserId(), request.amount());
        return ApiResponse.created(response, "포인트 충전(결제) 성공");
    }
}
