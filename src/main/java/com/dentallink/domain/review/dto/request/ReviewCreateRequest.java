package com.dentallink.domain.review.dto.request;

public record ReviewCreateRequest (
        Integer point,
        String content
) {}
