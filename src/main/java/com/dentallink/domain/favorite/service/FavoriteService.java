package com.dentallink.domain.favorite.service;

import com.dentallink.domain.favorite.enums.FavoriteAction;
import com.dentallink.domain.user.dto.security.AuthUser;

public interface FavoriteService {
    FavoriteAction toggleFavorite(Long hospitalId, AuthUser authUser);
}
