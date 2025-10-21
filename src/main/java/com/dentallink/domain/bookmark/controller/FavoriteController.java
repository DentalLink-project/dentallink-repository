package com.dentallink.domain.bookmark.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.domain.bookmark.service.FavoriteService;
import com.dentallink.domain.user.dto.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dentallink.common.response.ApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals/{hospitalId}/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> toggleFavorite(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                null,
                favoriteService.toggleFavorite(hospitalId, authUser)
        );
    }
}
