package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long amount,
        PaymentStatus paymentStatus,
        String orderId,
        String paymentKey,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        PaymentPointAccountResponse account
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getRequestedAt(),
                payment.getApprovedAt(),
                PaymentPointAccountResponse.from(payment.getPointAccount())
        );
    }
}
