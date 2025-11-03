package com.dentallink.domain.paymentLog.dto.response;

import com.dentallink.domain.paymentLog.entity.PaymentLog;
import com.dentallink.domain.paymentLog.enums.PaymentLogStatus;

public record PaymentLogResponse(
        Long id,
        PaymentLogStatus status,
        Long amount,
        String message,
        String orderId,
        PaymentLogPaymentResponse payment
) {
    public static PaymentLogResponse from(PaymentLog log) {
        return new PaymentLogResponse(
                log.getId(),
                log.getStatus(),
                log.getAmount(),
                log.getMessage(),
                log.getOrderId(),
                PaymentLogPaymentResponse.from(log.getPayment())
        );
    }
}
