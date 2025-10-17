package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import java.time.LocalDateTime;

public record PointAccountGetResponse(
        Long balance,
        LocalDateTime updatedAt) {
    public static PointAccountGetResponse from(PointAccount pointAccount) {
        return new PointAccountGetResponse(
                pointAccount.getBalance(),
                pointAccount.getUpdatedAt());
    }
}