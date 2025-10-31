package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.payment.entity.Payment;

public record PaymentCancelResponse (
        String orderId,
        Long amount
) {
    public static PaymentCancelResponse from(Payment payment) {
        return new PaymentCancelResponse(
                payment.getOrderId(),
                payment.getAmount()
        );
    }
}

