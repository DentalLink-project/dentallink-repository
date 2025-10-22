package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;

public record PaymentResponse(
        Long paymentId,
        Long amount,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        PaymentPointAccountResponse account
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                PaymentPointAccountResponse.from(payment.getPointAccount())
        );
    }
}
