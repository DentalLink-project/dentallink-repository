package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewListResponse (
        Long id,
        Long reservationId,
        Long hospitalId,
        Long userId,
        Integer point,
        String content
) {
    public static ReviewListResponse from(Review review) {
        return new ReviewListResponse(
                review.getId(),
                review.getReservationId(),
                review.getHospitalId(),
                review.getUserId(),
                review.getPoint(),
                review.getContent()
        );
    }
}
