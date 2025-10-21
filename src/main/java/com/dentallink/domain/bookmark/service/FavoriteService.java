package com.dentallink.domain.bookmark.service;

import com.dentallink.domain.user.dto.security.AuthUser;

public interface FavoriteService {
    String toggleFavorite(Long hospitalId, AuthUser authUser);
}
