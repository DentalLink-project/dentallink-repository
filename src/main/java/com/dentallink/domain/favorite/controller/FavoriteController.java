package com.dentallink.domain.favorite.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.favorite.enums.FavoriteAction;
import com.dentallink.domain.favorite.service.FavoriteService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals/{hospitalId}/favorites")
@Tag(name = "즐겨찾기", description = "즐겨찾기 관리를 위한 API.")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "즐겨찾기 토글",
            description = "병원에 대한 특정 사용자의 즐겨찾기를 생성/삭제함.",
            security = {@SecurityRequirement(name = "sessionAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "생성/삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "병원 존재하지 않음"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PostMapping
    public ResponseEntity<CommonApiResponse<Boolean>> toggleFavorite(
            @Parameter(description = "즐겨찾기할 병원 ID")
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        FavoriteAction action = favoriteService.toggleFavorite(hospitalId, authUser);
        return success(
                action.getIsFavorite(),
                action.getMessage()
        );
    }
}
