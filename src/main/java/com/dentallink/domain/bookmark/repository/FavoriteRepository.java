package com.dentallink.domain.bookmark.repository;

import com.dentallink.domain.bookmark.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite,Long> {
}
