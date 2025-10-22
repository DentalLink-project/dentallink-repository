package com.dentallink.domain.favorite.service;

import com.dentallink.domain.favorite.entity.Favorite;
import com.dentallink.domain.favorite.enums.FavoriteAction;
import com.dentallink.domain.favorite.repository.FavoriteRepository;
import com.dentallink.domain.hospital.service.HospitalInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.service.UserInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserInternalService userInternalService;
    private final HospitalInternalService hospitalInternalService;

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
                        userInternalService.getUserById(authUser.getUserId()),
                        hospitalInternalService.getHospitalById(hospitalId)
                )
        );
        return FavoriteAction.ADDED;
    }
}
