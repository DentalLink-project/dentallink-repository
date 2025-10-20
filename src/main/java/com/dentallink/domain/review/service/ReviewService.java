package com.dentallink.domain.review.service;

import com.dentallink.domain.review.dto.response.ReviewListResponse;
import com.dentallink.domain.review.entity.Review;
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

    @Transactional(readOnly = true)
    public Page<ReviewListResponse> findAllReviews(Long hospitalId, @PageableDefault Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByHospitalId(hospitalId, pageable);
        return reviews.map(ReviewListResponse::from);
    }
}
