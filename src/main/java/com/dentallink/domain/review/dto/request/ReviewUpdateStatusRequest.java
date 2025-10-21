package com.dentallink.domain.review.dto.request;

import com.dentallink.domain.review.enums.ReviewStatus;

public record ReviewUpdateStatusRequest(
        ReviewStatus status
) {}
