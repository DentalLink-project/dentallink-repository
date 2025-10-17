package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import java.time.LocalDateTime;

public record PointAccountDepositResponse(
        PointAccountType type,
        Long amount,
        Long balance,
        LocalDateTime createdAt) {
    public static PointAccountDepositResponse from(PointAccount pointAccount, Long amount) {
        return new PointAccountDepositResponse(
                PointAccountType.DEPOSIT,
                amount,
                pointAccount.getBalance(),
                pointAccount.getCreatedAt()
        );
    }
}
