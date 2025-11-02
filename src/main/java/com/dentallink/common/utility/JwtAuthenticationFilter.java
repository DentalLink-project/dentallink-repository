package com.dentallink.common.utility;

import com.dentallink.common.security.JwtAuthenticationToken;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String tokenValue = jwtTokenProvider.getJwtFromHeader(request);

        if (tokenValue != null) {
            // 블랙리스트 확인
            String blacklisted = redisTemplate.opsForValue().get(tokenValue);
            if (!ObjectUtils.isEmpty(blacklisted)) {
                log.error("로그아웃된 토큰입니다.");
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다.");
                return;
            }

            try {
                Claims info = jwtTokenProvider.getUserInfoFromToken(tokenValue);
                setAuthentication(info);
            } catch (SecurityException | MalformedJwtException e) {
                log.error("유효하지 않은 JWT 서명입니다.", e);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 서명입니다.");
                return;
            } catch (ExpiredJwtException e) {
                log.error("만료된 JWT 토큰입니다.", e);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "만료된 JWT 토큰입니다.");
                return;
            } catch (UnsupportedJwtException e) {
                log.error("지원되지 않는 JWT 토큰입니다.", e);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "지원되지 않는 JWT 토큰입니다.");
                return;
            } catch (IllegalArgumentException e) {
                log.error("JWT 클레임이 비어있습니다.", e);
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "JWT 클레임이 비어있습니다.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // 인증 처리
    private void setAuthentication(Claims claims) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        UserRole role = UserRole.valueOf(claims.get(JwtTokenProvider.AUTHORIZATION_KEY, String.class));
        AuthUser authUser = new AuthUser(userId, email, role);
        Authentication authentication = new JwtAuthenticationToken(authUser);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    // 에러 응답 전송
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
