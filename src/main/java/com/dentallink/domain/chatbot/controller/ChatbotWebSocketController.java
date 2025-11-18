package com.dentallink.domain.chatbot.controller;

import com.dentallink.common.security.JwtAuthenticationToken;
import com.dentallink.domain.chatbot.dto.ChatRequest;
import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.service.ChatbotService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 챗봇 WebSocket Controller
 * - 실시간 채팅 메시지 처리 (WebSocket)
 * - 프로덕션 환경에서 사용
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatbotWebSocketController {

    private final ChatbotService chatbotService;
    private final SimpMessagingTemplate messagingTemplate;

    // ===== WebSocket 메시지 핸들러 =====

    /**
     * 채팅 메시지 전송 (WebSocket)
     * 클라이언트: /app/chat/send
     * 응답: /queue/reply (null일 수 있음 - 상담 모드에서는 응답 없음)
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload @Valid ChatRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getUserIdFromHeader(headerAccessor);

        log.info("채팅 메시지 수신: userId={}, sessionId={}, content={}",
                userId, request.sessionId(), request.content());

        try {
            // 서비스에서 모든 로직 처리 (상담원 모드 포함)
            ChatResponse response = chatbotService.processMessage(request, userId);

            // null이 아닌 경우만 사용자에게 응답 전송 (상담 모드에서는 null)
            if (response != null) {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/reply",
                        response
                );
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);

            ChatResponse errorResponse = ChatResponse.builder()
                    .sessionId(request.sessionId())
                    .content("죄송합니다. 오류가 발생했습니다: " + e.getMessage())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reply",
                    errorResponse
            );
        }
    }

    /**
     * 세션 종료 (WebSocket)
     * 클라이언트: /app/chat/close
     */
    @MessageMapping("/chat/close")
    public void closeSession(
            @Payload Long sessionId,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getUserIdFromHeader(headerAccessor);

        log.info("세션 종료 요청: userId={}, sessionId={}", userId, sessionId);

        try {
            chatbotService.closeSession(sessionId, userId);

        } catch (Exception e) {
            log.error("세션 종료 중 오류 발생", e);
        }
    }

    /**
     * Typing 이벤트 (상대방에게 "입력 중..." 표시)
     * 클라이언트: /app/chat/typing
     */
    @MessageMapping("/chat/typing")
    @SendTo("/topic/typing")
    public TypingEvent handleTyping(
            @Payload Long sessionId,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getUserIdFromHeader(headerAccessor);
        return new TypingEvent(sessionId, userId, true);
    }

    // ===== Private Helper Methods =====

    /**
     * 헤더에서 사용자 ID 추출 (타입 체크 강화)
     */
    private Long getUserIdFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        // 타입 체크 후 안전하게 캐스팅
        if (!(principal instanceof JwtAuthenticationToken jwtToken)) {
            throw new IllegalArgumentException("잘못된 인증 타입입니다. JwtAuthenticationToken이 필요합니다.");
        }

        Object principalObj = jwtToken.getPrincipal();

        if (!(principalObj instanceof AuthUser authUser)) {
            throw new IllegalArgumentException("잘못된 Principal 타입입니다. AuthUser가 필요합니다.");
        }

        return authUser.getUserId();
    }

    // ===== Inner Classes =====

    /**
     * Typing 이벤트
     */
    private record TypingEvent(Long sessionId, Long userId, boolean isTyping) {
    }
}