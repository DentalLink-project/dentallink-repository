package com.dentallink.domain.bookmark.controller;

import com.dentallink.domain.bookmark.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals/{hospitalId}/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
}
