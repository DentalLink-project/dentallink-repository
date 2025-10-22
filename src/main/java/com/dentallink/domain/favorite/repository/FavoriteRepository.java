package com.dentallink.domain.favorite.repository;

import com.dentallink.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite,Long> {
    Optional<Favorite> findByHospitalIdAndUserId(Long hospitalId, Long userId);
}
