package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.exception.ReviewErrorCode;
import com.dentallink.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewInternalService {


    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Review getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<Review> findOptionalById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    @Transactional(readOnly = true)
    public boolean existsByReservationId(Long reservationId) {
        return reviewRepository.existsByReservationId(reservationId);
    }

    @Transactional(readOnly = true)
    public Page<Review> findReviewsByHospitalId(Long hospitalId, Pageable pageable) {
        return reviewRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional
    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    // Optional: 병원 관리자 확인 (ExternalService에서 호출 가능)
    @Transactional(readOnly = true)
    public boolean isAdmin(Long hospitalId, Long userId) {
        // TODO: HospitalRepository 등으로 관리자 확인 로직 구현
        return true; // 임시
    }
}
