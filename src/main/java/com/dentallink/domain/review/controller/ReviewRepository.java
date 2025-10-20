package com.dentallink.domain.review.controller;

import com.dentallink.domain.review.entity.Review;
import org.springframework.data.repository.Repository;

interface ReviewRepository extends Repository<Review, Long> {
}
