package com.dentallink.domain.review.controller;

import com.dentallink.domain.review.dto.response.ReviewDetailResponse;
import com.dentallink.domain.review.dto.response.ReviewListResponse;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewService reviewService;

    // 병원 별 리뷰 목록 조회
    @GetMapping("/{hospitalId}")
    public ResponseEntity<Page<ReviewListResponse>> getAllReviews(
            @PathVariable Long hospitalId,
            @PageableDefault Pageable pageable
    ) {
        Page<ReviewListResponse> reviews = reviewService.findAllReviews(hospitalId, pageable);
        return ResponseEntity.ok(reviews);
    }

    // 리뷰 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetailResponse> getReviewById(
            @PathVariable Long id
    ) {
        ReviewDetailResponse review = reviewService.findReviewById(id);
        return ResponseEntity.ok(review);
    }
}
