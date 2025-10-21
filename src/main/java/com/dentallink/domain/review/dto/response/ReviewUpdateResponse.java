package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewUpdateResponse (
    Long id,
    Integer point,
    String content,
    LocalDateTime updatedAt
) {
    public static ReviewUpdateResponse of(Review review){
        return new ReviewUpdateResponse(
                review.getId(),
                review.getPoint(),
                review.getContent(),
                review.getUpdatedAt()
        );
    }
}
