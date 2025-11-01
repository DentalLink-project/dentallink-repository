package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.payment.entity.Payment;

public record PaymentReadyResponse(
        String orderId,
        Long amount
) {
    public static PaymentReadyResponse from(Payment payment) {
        return new PaymentReadyResponse(
                payment.getOrderId(),
                payment.getAmount()
        );
    }
}
