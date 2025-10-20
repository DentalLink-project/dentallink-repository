package com.dentallink.domain.pointLog.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;

public record PointLogPointAccountResponse(
        Long pointAccountId,
        Long balance
) {
    public static PointLogPointAccountResponse from(PointAccount account) {
        return new PointLogPointAccountResponse(
                account.getId(),
                account.getBalance()
        );
    }
}
