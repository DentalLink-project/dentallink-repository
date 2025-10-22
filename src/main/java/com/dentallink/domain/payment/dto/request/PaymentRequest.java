package com.dentallink.domain.payment.dto.request;

import com.dentallink.domain.payment.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public record PaymentRequest (
        @NotNull(message = "금액을 입력해주세요.")
        @Min(value = 1000, message = "최소 금액은 1000원입니다.")
        Long amount,

        @NotNull(message = "결제 수단을 선택해주세요.")
        PaymentMethod method
){
}
