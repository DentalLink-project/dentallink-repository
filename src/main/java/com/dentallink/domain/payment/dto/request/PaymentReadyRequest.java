package com.dentallink.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentReadyRequest(
        @NotNull(message = "금액을 입력해주세요.")
        @Min(value = 1000, message = "최소 결제 금액은 1000원 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "주문 ID(orderId)는 필수입니다.")
        String orderId // 프론트에서 생성한 고유 주문번호
) {
}
