package com.dentallink.domain.bookmark.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FavoriteAction {
    ADDED("추가"),
    REMOVED("삭제");

    private final String message;
}
