package com.dentallink.domain.pointAccount.dto.request;

public record PointAccountWithdrawRequest(
        Long amount,
        String bankName,
        String accountNumber,
        String accountHolder
) {}
