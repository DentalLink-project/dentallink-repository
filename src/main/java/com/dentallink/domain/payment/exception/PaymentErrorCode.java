package com.dentallink.domain.payment.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    NOT_FOUND_ORDER_ID(HttpStatus.NOT_FOUND, "해당 orderId가 존재하지 않습니다."),
    DUPLICATE_ORDER_ID(HttpStatus.CONFLICT, "이미 존재하는 주문 ID입니다."),
    PAYMENT_RESPONSE_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "Toss 결제 승인 응답이 비어 있습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "READY 상태의 결제만 취소할 수 있습니다."),
    PAYMENT_APPROVAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "결제 승인 처리 중 오류가 발생했습니다."),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "해당 주문 ID로 결제 내역을 찾을 수 없습니다.");
    private final HttpStatus httpStatus;
    private final String message;
}