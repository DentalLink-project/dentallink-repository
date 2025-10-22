package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalRepository;
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
    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public Review getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(ReviewErrorCode.REVIEW_NOT_FOUND));
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

    @Transactional
    public void deleteReview(Review review) {
        reviewRepository.save(review); // soft delete 처리 시
    }

    @Transactional(readOnly = true)
    public Optional<Review> findOptionalById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

//    @Transactional(readOnly = true)
//    public boolean isAdmin(Long hospitalId, Long userId) {
//        Hospital hospital = hospitalRepository.findById(hospitalId)
//                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
//        return hospital.getUserId().equals(userId); // 병원 소유자가 관리자
//    }
}
