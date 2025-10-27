package com.dentallink.domain.auth.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.utility.JwtUtil;
import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.service.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceImpl authService;

    // 로그인 로직
    @PostMapping("/login")
    public ResponseEntity<CommonApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        String token = authService.login(request).token();
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);
        return success(
                null,
                "로그인 성공"
        );
    }

    // 토큰 재발급
//    @PostMapping("/refresh-token")
//    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
//            @RequestHeader("Authorization") String refreshToken
//    ) {
//        String token = refreshToken.substring(7);
//        return success(
//                authService.refreshToken(token),
//                "토큰이 갱신되었습니다."
//        );
//    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<CommonApiResponse<Void>> logout(
            @RequestHeader(JwtUtil.AUTHORIZATION_HEADER) String authorizationHeader
    ) {
        if (authorizationHeader != null && authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {
        String accessToken = authorizationHeader.substring(JwtUtil.BEARER_PREFIX.length());
        authService.logout(accessToken);
    }
        return success(
                null,
                "로그아웃 되었습니다."
        );
    }
}
