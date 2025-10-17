package com.dentallink.domain.pointAccount.dto.request;

import lombok.NonNull;

public record PointAccountRequest(
        @NonNull Long amount
) {}