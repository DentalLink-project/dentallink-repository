package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.review.dto.request.*;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.exception.ReviewErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewExternalService {

    private final ReviewInternalService reviewInternalService;

    // 리뷰 등록
    @Transactional
    public ReviewCreateResponse createReview(
            Long reservationId,
            Long hospitalId,
            Long userId,
            ReviewCreateRequest request
    ) {
        if (reviewInternalService.existsByReservationId(reservationId)) {
            throw new GlobalException(ReviewErrorCode.DUPLICATE_REVIEW);
        }

        Review review = Review.of(reservationId, hospitalId, userId, request.point(), request.content());
        Review saved = reviewInternalService.saveReview(review);

        return ReviewCreateResponse.of(saved);
    }

    // 병원 리뷰 조회
    @Transactional(readOnly = true)
    public Page<ReviewListResponse> findAllReviews(Long hospitalId, Pageable pageable) {
        return reviewInternalService.findReviewsByHospitalId(hospitalId, pageable)
                .map(ReviewListResponse::from);
    }

    // 리뷰 단건 조회
    @Transactional(readOnly = true)
    public ReviewDetailResponse findReviewById(Long reviewId) {
        Review review = reviewInternalService.getReviewById(reviewId);
        return ReviewDetailResponse.of(review);
    }

    // 리뷰 수정
    @Transactional
    public ReviewUpdateResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review = reviewInternalService.getReviewById(reviewId);

        if (!review.getUserId().equals(userId)) {
            throw new GlobalException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }

        if (review.isDeleted()) {
            throw new GlobalException(ReviewErrorCode.ALREADY_DELETED);
        }

        review.update(request.point(), request.content());
        Review updated = reviewInternalService.saveReview(review);

        return ReviewUpdateResponse.of(updated);
    }

    // 리뷰 삭제
    @Transactional
    public ReviewDeleteResponse deleteReview(Long reviewId, Long userId) {
        Review review = reviewInternalService.getReviewById(reviewId);

        if (!review.getUserId().equals(userId)) {
            throw new GlobalException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }

        if (review.isDeleted()) {
            throw new GlobalException(ReviewErrorCode.ALREADY_DELETED);
        }

        review.delete();
        reviewInternalService.deleteReview(review);

        return ReviewDeleteResponse.of(review);
    }

    // 리뷰 상태 변경
    @Transactional
    public ReviewStatusResponse updateReviewStatus(
            Long reviewId,
            ReviewUpdateStatusRequest request,
            Long hospitalAdminId) {

        Review review = reviewInternalService.getReviewById(reviewId);

        validateHospitalAdmin(review.getHospitalId(), hospitalAdminId);

        switch (request.status()) {
            case APPROVED -> review.approve();
            case REJECTED -> review.reject();
            default -> throw new GlobalException(ReviewErrorCode.INVALID_STATUS_TRANSITION);
        }

        Review updated = reviewInternalService.saveReview(review);
        return ReviewStatusResponse.of(updated);
    }

    // TODO: 병원 관리자 검증 로직
    private void validateHospitalAdmin(Long hospitalId, Long adminId) {
//        if (!reviewInternalService.isAdmin(hospitalId, adminId)) {
//            throw new GlobalException(ReviewErrorCode.NOT_HOSPITAL_ADMIN);
//        }
    }
}
