package com.dentallink.domain.bookmark.service;

import com.dentallink.domain.bookmark.enums.FavoriteAction;
import com.dentallink.domain.user.dto.security.AuthUser;

public interface FavoriteService {
    FavoriteAction toggleFavorite(Long hospitalId, AuthUser authUser);
}
