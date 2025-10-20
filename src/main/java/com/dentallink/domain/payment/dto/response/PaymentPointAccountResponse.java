package com.dentallink.domain.payment.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;

public record PaymentPointAccountResponse(
        Long pointAccountId,
        Long balance
) {
    public static PaymentPointAccountResponse from(PointAccount account) {
        return new PaymentPointAccountResponse(
                account.getId(),
                account.getBalance()
        );
    }
}
