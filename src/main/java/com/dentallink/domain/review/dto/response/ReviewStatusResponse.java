package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.enums.ReviewStatus;

import java.time.LocalDateTime;

public record ReviewStatusResponse(
        Long id,
        ReviewStatus status,
        LocalDateTime updatedAt
) {
    public static ReviewStatusResponse of(Review review) {
        return new ReviewStatusResponse(
                review.getId(),
                review.getStatus(),
                review.getUpdatedAt()
        );
    }
}
