package com.dentallink.domain.favorite.service;

import com.dentallink.domain.favorite.entity.Favorite;
import com.dentallink.domain.favorite.enums.FavoriteAction;
import com.dentallink.domain.favorite.repository.FavoriteRepository;
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
    public FavoriteAction toggleFavorite(Long hospitalId, AuthUser authUser) {
        Optional<Favorite> favorite = favoriteRepository.findByHospitalIdAndUserId(hospitalId, authUser.getUserId());
        if (favorite.isPresent()) {
            favoriteRepository.delete(favorite.get());
            return FavoriteAction.REMOVED;
        }
        favoriteRepository.save(
                Favorite.of(
                        userQueryService.getUserById(authUser.getUserId()),
                        hospitalService.getHospitalById(hospitalId)
                )
        );
        return FavoriteAction.ADDED;
    }
}
