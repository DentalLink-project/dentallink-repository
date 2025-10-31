package com.dentallink.domain.review.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateStatusRequest;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.service.ReviewInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.ApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {
    private final ReviewInternalService reviewInternalService;

    // 리뷰 등록
    @PostMapping("/hospitals/{hospitalId}/reviews")
    public ResponseEntity<ApiResponse<ReviewCreateResponse>> createReview(
            @PathVariable Long hospitalId,
            @RequestParam Long reservationId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        ReviewCreateResponse response = reviewInternalService.createReview(
                reservationId, hospitalId, authUser.getUserId(), reviewCreateRequest
        );
        return created (
                response, "리뷰가 작성되었습니다."
        );
    }

    // 병원 별 리뷰 목록 조회
    @GetMapping("/hospitals/{hospitalId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewListResponse>>> getAllReviews(
            @PathVariable Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return success (
                reviewInternalService.findAllReviews(hospitalId, page, size),
                "리뷰 목록이 조회되었습니다."
        );
    }

    // 리뷰 상세 조회
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> getReviewById(
            @PathVariable Long id
    ) {
        ReviewDetailResponse review = reviewInternalService.findReviewById(id);
        return success (
                review, "리뷰가 조회되었습니다."
        );
    }

    // 리뷰 수정
    @PatchMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewUpdateResponse>> updateReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReviewUpdateRequest reviewUpdateRequest
    ) {
        ReviewUpdateResponse review = reviewInternalService.updateReview(id, authUser.getUserId(), reviewUpdateRequest);
        return success (
                review, "리뷰가 수정되었습니다."
        );
    }

    // 리뷰 삭제
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewDeleteResponse>> deleteReviewById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        ReviewDeleteResponse review = reviewInternalService.deleteReview(id, authUser.getUserId());
        return success (
                review, "리뷰가 삭제되었습니다."
        );
    }

    // 리뷰 상태 변경
    @PatchMapping("/reviews/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> updateReviewStatus(
            @PathVariable Long id,
            @RequestBody ReviewUpdateStatusRequest request,
            @AuthenticationPrincipal AuthUser authUser // 관리자 인증정보
    ) {
        return success (
                reviewInternalService.updateReviewStatus(id, request),
                "리뷰 상태가 변경되었습니다."
        );
    }
}
