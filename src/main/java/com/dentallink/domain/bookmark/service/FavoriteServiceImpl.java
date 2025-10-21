package com.dentallink.domain.bookmark.service;

import com.dentallink.domain.bookmark.entity.Favorite;
import com.dentallink.domain.bookmark.enums.FavoriteAction;
import com.dentallink.domain.bookmark.repository.FavoriteRepository;
import com.dentallink.domain.hospital.service.HospitalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserQueryService userQueryService;
    private final HospitalService hospitalService;

    @Transactional
    @Override
    public String toggleFavorite(Long hospitalId, AuthUser authUser) {
        Optional<Favorite> favorite = favoriteRepository.findByHospitalIdAndUserId(hospitalId, authUser.getUserId());
        if (favorite.isPresent()) {
            favoriteRepository.delete(favorite.get());
            return FavoriteAction.REMOVED.getMessage();
        }
        favoriteRepository.save(
                Favorite.of(
                        userQueryService.getUserById(authUser.getUserId()),
                        hospitalService.getHospitalById(hospitalId)
                )
        );
        return FavoriteAction.ADDED.getMessage();
    }
}
