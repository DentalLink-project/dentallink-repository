package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import java.time.LocalDateTime;

public record PointAccountRefundResponse(
        PointAccountType type,
        Long amount,
        Long balance,
        LocalDateTime createdAt) {
    public static PointAccountRefundResponse from(PointAccount pointAccount, Long amount) {
        return new PointAccountRefundResponse(
                PointAccountType.REFUND,
                amount,
                pointAccount.getBalance(),
                pointAccount.getCreatedAt()
        );
    }
}