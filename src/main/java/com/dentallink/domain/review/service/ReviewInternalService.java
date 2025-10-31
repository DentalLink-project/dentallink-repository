package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateStatusRequest;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.exception.ReviewErrorCode;
import com.dentallink.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewInternalService {
    private final ReviewRepository reviewRepository;

    // 리뷰 등록
    @Transactional
    public ReviewCreateResponse createReview(
            Long reservationId,
            Long hospitalId,
            Long userId,
            ReviewCreateRequest request
    ) {
        if (reviewRepository.existsByReservationId(reservationId)) {
            throw new GlobalException(ReviewErrorCode.DUPLICATE_REVIEW);
        }

        Review review = Review.of(reservationId, hospitalId, userId, request.point(), request.content());
        Review saved = reviewRepository.save(review);

        return ReviewCreateResponse.of(saved);
    }

    // 병원 리뷰 조회
    @Transactional(readOnly = true)
    public PageResponse<ReviewListResponse> findAllReviews(Long hospitalId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findByHospitalId(hospitalId, pageable);
        Page<ReviewListResponse> reviewListResponse = reviews.map(ReviewListResponse::from);
        return PageResponse.fromPage(reviewListResponse);
    }

    // 리뷰 단건 조회
    @Transactional(readOnly = true)
    public ReviewDetailResponse findReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));
        return ReviewDetailResponse.of(review);
    }

    // 리뷰 수정
    @Transactional
    public ReviewUpdateResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review = validateReviewOwner(reviewId, userId);

        review.update(request.point(), request.content());
        Review updated = reviewRepository.save(review);

        return ReviewUpdateResponse.of(updated);
    }

    // 리뷰 삭제
    @Transactional
    public ReviewDeleteResponse deleteReview(Long reviewId, Long userId) {
        Review review = validateReviewOwner(reviewId, userId);

        review.delete(); // soft delete 상태 변경
        reviewRepository.save(review);

        return ReviewDeleteResponse.of(review);
    }

    // 리뷰 상태 변경
    @Transactional
    public ReviewStatusResponse updateReviewStatus(Long reviewId, ReviewUpdateStatusRequest request, Long hospitalAdminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));

        validateHospitalAdmin(review.getHospitalId(), hospitalAdminId);

        switch (request.status()) {
            case APPROVED -> review.approve();
            case REJECTED -> review.reject();
            default -> throw new GlobalException(ReviewErrorCode.INVALID_STATUS_TRANSITION);
        }

        Review updated = reviewRepository.save(review);
        return ReviewStatusResponse.of(updated);
    }

    // 리뷰 소유자 검증 및 삭제 여부 확인
    private Review validateReviewOwner(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));

        if (!Objects.equals(review.getUserId(), userId)) {
            throw new GlobalException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }

        if (review.isDeleted()) {
            throw new GlobalException(ReviewErrorCode.ALREADY_DELETED);
        }

        return review;
    }

    // TODO: 병원 관리자 검증 로직
    private void validateHospitalAdmin(Long hospitalId, Long adminId) {
        // TODO: HospitalRepository 등으로 관리자 확인 로직 구현
        boolean isAdmin = true; // 임시
        if (!isAdmin) {
            throw new GlobalException(ReviewErrorCode.NOT_HOSPITAL_ADMIN);
        }
    }
}
