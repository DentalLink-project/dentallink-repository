package com.dentallink.domain.pointAccount.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record PointAccountWithdrawRequest(
        @NotNull(message = "출금할 금액을 입력해주세요.")
        @Min(value = 1, message = "출금 금액은 1원 이상이어야 합니다.")
        Long amount,
        @NotBlank(message = "은행명을 입력해주세요.")
        String bankName,
        @NotBlank(message = "계좌번호를 입력해주세요.")
        String accountNumber,
        @NotBlank(message = "예금주명을 입력해주세요.")
        String accountHolder
) {}
