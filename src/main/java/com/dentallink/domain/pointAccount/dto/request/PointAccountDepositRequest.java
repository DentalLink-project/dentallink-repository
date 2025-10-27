package com.dentallink.domain.pointAccount.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointAccountDepositRequest(
        @NotNull(message = "계좌 ID를 입력해주세요.")
        Long accountId,

        @NotNull(message = "금액을 입력해주세요.")
        @Min(value = 1000, message = "최소 금액은 1000원입니다.")
        Long amount
) {}
