package com.dentallink.domain.payment.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.payment.dto.request.PaymentRequest;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.service.PaymentInternalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentInternalService paymentInternalService;

    /*@PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PaymentResponse>> depositPoint(
            // 유저 아이디로 받은 account를 넘긴다.ㅇㄴㄹ.ㅜㅏㄴㅇ륳
            @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentInternalService.depositPoint(request.accountId(), request.amount());
        return ApiResponse.success(response, "포인트 충전에 성공했습니다.");
    }*/
}
