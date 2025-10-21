package com.dentallink.domain.bookmark.repository;

import com.dentallink.domain.bookmark.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite,Long> {
    Optional<Favorite> findByHospitalIdAndUserId(Long hospitalId, Long userId);
}
