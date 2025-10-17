package com.dentallink.domain.pointLog.controller;

import com.dentallink.domain.pointLog.service.PointLogInternalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/point-log")
public class PointLogController {
    private final PointLogInternalService pointLogInternalService;
}
