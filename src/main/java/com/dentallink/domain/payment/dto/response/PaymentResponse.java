package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.payment.entity.Payment;

public record PaymentResponse(
        Long paymentId,
        Long amount,
        PaymentPointAccountResponse account
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                PaymentPointAccountResponse.from(payment.getPointAccount())
        );
    }
}
