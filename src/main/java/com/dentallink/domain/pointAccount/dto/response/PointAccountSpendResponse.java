package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import java.time.LocalDateTime;

public record PointAccountSpendResponse(
        PointAccountType type,
        Long amount,
        Long balance,
        LocalDateTime createdAt) {
    public static PointAccountSpendResponse from(PointAccount pointAccount, Long amount) {
        return new PointAccountSpendResponse(
                PointAccountType.SPEND,
                amount,
                pointAccount.getBalance(),
                pointAccount.getCreatedAt()
        );
    }
}