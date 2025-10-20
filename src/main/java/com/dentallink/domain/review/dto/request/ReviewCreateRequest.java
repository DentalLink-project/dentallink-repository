package com.dentallink.domain.review.dto.request;

public record ReviewCreateRequest (
        Long reservationId,
        Long hospitalId,
        Long userId,
        Integer point,
        String content
) {}
