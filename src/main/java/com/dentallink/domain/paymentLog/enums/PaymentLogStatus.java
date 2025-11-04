package com.dentallink.domain.paymentLog.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentLogStatus {
    READY("결제 대기 중"),
    SUCCESS("결제 성공"),
    FAILED("결제 실패"),
    CANCELLED("결제 취소");

    private final String description;

    public boolean isFinished() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
