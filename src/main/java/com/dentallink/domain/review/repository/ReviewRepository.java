package com.dentallink.domain.review.repository;

import com.dentallink.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {
    @Query("SELECT r FROM Review r WHERE r.hospitalId = :hospitalId ORDER BY r.createdAt")
    Page<Review> findByHospitalId(Long hospitalId, Pageable pageable);
}
