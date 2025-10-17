package com.dentallink.domain.pointLog.dto.response;

import com.dentallink.domain.pointLog.entity.PointLog;
import com.dentallink.domain.pointLog.enums.PointLogType;

public record PointLogResponse(
        Long id,
        PointLogType type,
        Long amount,
        Long balanceAfter,
        PointLogPointAccountResponse account

){
    public static PointLogResponse from(PointLog pointLog) {
        return new PointLogResponse(
                pointLog.getId(),
                pointLog.getType(),
                pointLog.getAmount(),
                pointLog.getBalanceAfter(),
                PointLogPointAccountResponse.from(pointLog)
        );
    }
}
