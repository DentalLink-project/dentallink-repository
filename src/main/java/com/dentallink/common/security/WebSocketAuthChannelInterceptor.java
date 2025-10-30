package com.dentallink.common.security;

import com.dentallink.common.utility.JwtUtil;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // STOMP 연결 요청 (CONNECT)일 때만 JWT 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 1. Authorization 헤더에서 토큰 추출
            String token = accessor.getFirstNativeHeader(JwtUtil.AUTHORIZATION_HEADER);

            if (StringUtils.hasText(token) && token.startsWith(JwtUtil.BEARER_PREFIX)) {

                String tokenValue = token.substring(JwtUtil.BEARER_PREFIX.length());

                try {
                    // 2. JWT 유효성 검증 및 Claims 추출 (JwtUtil 재사용)
                    Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

                    // 3. AuthUser 및 JwtAuthenticationToken 생성
                    Long userId = Long.valueOf(info.getSubject());
                    String email = info.get("email", String.class);
                    UserRole role = UserRole.valueOf(info.get(JwtUtil.AUTHORIZATION_KEY, String.class));

                    AuthUser authUser = new AuthUser(userId, email, role);
                    Authentication authentication = new JwtAuthenticationToken(authUser);

                    // 4. Principal을 세션에 설정
                    accessor.setUser(authentication);
                    log.info("WebSocket 인증 성공: userId={}", userId);

                } catch (Exception e) {
                    // 토큰 만료, 서명 오류 등 인증 실패
                    log.error("WebSocket JWT 인증 실패: {}", e.getMessage());
                    // 연결 거부 (에러 처리)
                    throw new RuntimeException("JWT 인증 실패: " + e.getMessage());
                }
            } else {
                // 토큰이 없거나 형식이 잘못된 경우 (개발 중에는 허용할 수도 있지만, 보안상 거부가 맞음)
                log.warn("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
            }
        }

        return message;
    }
}