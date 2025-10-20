package com.dentallink.domain.review.controller;

import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.response.ReviewCreateResponse;
import com.dentallink.domain.review.dto.response.ReviewDetailResponse;
import com.dentallink.domain.review.dto.response.ReviewListResponse;
import com.dentallink.domain.review.dto.response.ReviewUpdateResponse;
import com.dentallink.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 리뷰 등록
    @PostMapping
    public ResponseEntity<ReviewCreateResponse> createReview(
            @RequestParam Long reservationId,
            @RequestParam Long hospitalId,
            @RequestParam Long userId,
            @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        return ResponseEntity.ok(reviewService.createReview(reservationId, hospitalId, userId, reviewCreateRequest));
    }

    // 리뷰 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ReviewUpdateResponse> updateReview(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody ReviewUpdateRequest reviewUpdateRequest
    ) {
        ReviewUpdateResponse review = reviewService.updateReview(id, userId, reviewUpdateRequest);
        return ResponseEntity.ok(review);
    }
}
