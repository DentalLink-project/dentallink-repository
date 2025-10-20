package com.dentallink.domain.review.dto.response;

import com.dentallink.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewDetailResponse (
        Long id,
        Long hospitalId,
        Long userId,
        Long reservationId,
        Integer point,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewDetailResponse of(Review review) {
        return new ReviewDetailResponse(
                review.getId(),
                review.getHospitalId(),
                review.getUserId(),
                review.getHospitalId(),
                review.getPoint(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
