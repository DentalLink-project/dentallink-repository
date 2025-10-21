package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewCreateResponse (
    Long id,
    Long reservationId,
    Long hospitalId,
    Long userId,
    Integer point,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ReviewCreateResponse of(Review review) {
        return new ReviewCreateResponse(
                review.getId(),
                review.getReservationId(),
                review.getHospitalId(),
                review.getUserId(),
                review.getPoint(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
