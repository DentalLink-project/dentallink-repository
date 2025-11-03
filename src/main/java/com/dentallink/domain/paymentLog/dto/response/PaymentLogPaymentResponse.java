package com.dentallink.domain.paymentLog.dto.response;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;

public record PaymentLogPaymentResponse(
        Long paymentId,
        PaymentMethod method,
        PaymentStatus status,
        Long amount
) {
    public static PaymentLogPaymentResponse from(Payment payment) {
        return new PaymentLogPaymentResponse(
                payment.getId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount()
        );
    }
}
