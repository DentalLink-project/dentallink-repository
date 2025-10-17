package com.dentallink.domain.pointLog.dto.response;

import com.dentallink.domain.pointLog.entity.PointLog;

public record PointLogPointAccountResponse(
        Long pointAccountId,
        Long balance
) {
    public static PointLogPointAccountResponse from(PointLog pointLog) {
        return new PointLogPointAccountResponse(
                pointLog.getPointAccount().getId(),
                pointLog.getPointAccount().getBalance()
        );
    }
}
