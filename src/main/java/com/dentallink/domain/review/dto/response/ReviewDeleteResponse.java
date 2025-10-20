package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewDeleteResponse (
        Long id,
        Long userId,
        LocalDateTime deletedAt
) {
    public static ReviewDeleteResponse of(Review review) {
        return new ReviewDeleteResponse(
                review.getId(),
                review.getUserId(),
                review.getDeletedAt()
        );
    }
}
