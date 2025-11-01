package com.dentallink.domain.auth.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.utility.JwtTokenProvider;
import com.dentallink.domain.auth.dto.request.LoginRequest;
import com.dentallink.domain.auth.dto.response.JwtToken;
import com.dentallink.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "인증을 위한 API.")
public class AuthController {

    private final AuthService authService;

    // 로그인 로직
    @Operation(summary = "로그인",
            description = "이메일과 비밀번호를 사용해 로그인.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PostMapping("/login")
    public ResponseEntity<CommonApiResponse<Void>> login(
            @Parameter(description = "로그인하기 위한 정보")
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        JwtToken token = authService.login(request);
        response.addHeader(JwtTokenProvider.AUTHORIZATION_HEADER, token.getAccessToken());
        response.addHeader(JwtTokenProvider.REFRESH_TOKEN_HEADER, token.getRefreshToken());
        return success(
                null,
                "로그인 성공"
        );
    }

    // 토큰 재발급
    @PostMapping("/refresh-token")
    public ResponseEntity<CommonApiResponse<Void>> refreshToken(
            @RequestHeader(JwtTokenProvider.REFRESH_TOKEN_HEADER) String refreshToken,
            HttpServletResponse response
    ) {
        response.setHeader(JwtTokenProvider.AUTHORIZATION_HEADER,
                authService.reissueAccessToken(refreshToken).getAccessToken());
        return success(
                null,
                "토큰이 재발급되었습니다."
        );
    }

    // 로그아웃
    @Operation(summary = "로그아웃",
            description = "로그아웃 합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            })
    @PostMapping("/logout")
    public ResponseEntity<CommonApiResponse<Void>> logout(
            @RequestHeader(JwtTokenProvider.AUTHORIZATION_HEADER) String accessToken,
            @RequestHeader(JwtTokenProvider.REFRESH_TOKEN_HEADER) String refreshToken
    ) {
        if (accessToken != null && accessToken.startsWith(JwtTokenProvider.BEARER_PREFIX)) {
        String token = accessToken.substring(JwtTokenProvider.BEARER_PREFIX.length());
        authService.logout(token, refreshToken);
    }
        return success(
                null,
                "로그아웃 되었습니다."
        );
    }
}
