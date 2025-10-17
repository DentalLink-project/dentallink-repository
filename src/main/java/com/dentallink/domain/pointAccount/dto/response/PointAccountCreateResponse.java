package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import java.time.LocalDateTime;

public record PointAccountCreateResponse(
        Long accountId,
        Long balance,
        LocalDateTime createdAt) {
    public static PointAccountCreateResponse from(PointAccount pointAccount) {
        return new PointAccountCreateResponse(
                pointAccount.getId(),
                pointAccount.getBalance(),
                pointAccount.getCreatedAt());
    }
}
