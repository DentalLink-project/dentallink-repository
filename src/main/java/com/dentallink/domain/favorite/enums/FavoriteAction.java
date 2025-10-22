package com.dentallink.domain.favorite.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FavoriteAction {
    ADDED("병원이 즐겨찾기에 추가되었습니다.", true),
    REMOVED("즐겨찾기가 삭제되었습니다.", false);

    private final String message;
    private final Boolean isFavorite;
}
