package com.dentallink.domain.review.dto.request;

public record ReviewUpdateRequest (
    Integer point,
    String content
) {
}
