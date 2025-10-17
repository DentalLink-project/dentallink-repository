package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import java.time.LocalDateTime;

public record PointAccountWithdrawResponse(
        PointAccountType type,
        Long amount,
        Long balance,
        LocalDateTime createdAt) {
    public static PointAccountWithdrawResponse from(PointAccount pointAccount, Long amount) {
        return new PointAccountWithdrawResponse(
                PointAccountType.WITHDRAW,
                amount,
                pointAccount.getBalance(),
                pointAccount.getCreatedAt()
        );
    }
}