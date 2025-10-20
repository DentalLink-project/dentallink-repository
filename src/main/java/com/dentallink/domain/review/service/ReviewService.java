package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.response.ReviewCreateResponse;
import com.dentallink.domain.review.dto.response.ReviewDetailResponse;
import com.dentallink.domain.review.dto.response.ReviewListResponse;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.exception.ReviewErrorCode;
import com.dentallink.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    // 병원 별 리뷰 목록 조회
    @Transactional(readOnly = true)
    public Page<ReviewListResponse> findAllReviews(Long hospitalId, @PageableDefault Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByHospitalId(hospitalId, pageable);
        return reviews.map(ReviewListResponse::from);
    }

    // 리뷰 상세 조회 (단건)
    @Transactional(readOnly = true)
    public ReviewDetailResponse findReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));

        return ReviewDetailResponse.of(review);
    }

    // 리뷰 등록
    @Transactional
    public ReviewCreateResponse createReview(ReviewCreateRequest reviewCreateRequest) {
        Review review = new Review(
                reviewCreateRequest.reservationId(),
                reviewCreateRequest.hospitalId(),
                reviewCreateRequest.userId(),
                reviewCreateRequest.point(),
                reviewCreateRequest.content()
        );

        Review createReview = reviewRepository.save(review);

        return ReviewCreateResponse.of(createReview);
    }
}
