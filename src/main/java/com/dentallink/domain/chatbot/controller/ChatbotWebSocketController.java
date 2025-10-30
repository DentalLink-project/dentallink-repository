package com.dentallink.domain.chatbot.controller;

import com.dentallink.common.security.JwtAuthenticationToken;
import com.dentallink.domain.chatbot.dto.ChatRequest;
import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.enums.MessageType;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 WebSocket Controller
 * - 실시간 채팅 메시지 처리 (WebSocket)
 * - Postman 테스트용 REST API
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
     * 응답: /user/queue/reply
     */
    @MessageMapping("/chat/send")
    @SendTo("/queue/messages")
    public void sendMessage(
            @Payload @Valid ChatRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getUserIdFromHeader(headerAccessor);

        log.info("채팅 메시지 수신: userId={}, sessionId={}, content={}",
                userId, request.sessionId(), request.content());

        try {
            ChatResponse response = chatbotService.processMessage(request, userId);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reply",
                    response
            );

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

            ChatResponse response = ChatResponse.createSessionClosedResponse(sessionId);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reply",
                    response
            );

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

    // ===== REST API (Postman 테스트용) ===== ⭐

    @PostMapping("/api/chatbot/messages")
    @ResponseBody
    public ChatResponse sendMessageRest(
            @RequestBody @Valid ChatRequest request,
            @AuthenticationPrincipal AuthUser user) {

        Long userId = user.getUserId();
        log.info("REST API - 메시지 전송: userId={}, content={}",
                userId, request.content());

        return chatbotService.processMessage(request, userId);
    }

    /**
     * 🧪 Postman 테스트: 세션 종료 (REST API)
     * <p>
     * POST /api/chatbot/sessions/{sessionId}/close
     * Authorization: Bearer {JWT_TOKEN}
     */
    @PostMapping("/api/chatbot/sessions/{sessionId}/close")
    @ResponseBody
    public void closeSessionRest(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthUser user) { // ✅ Principal -> AuthUser로 수정

        Long userId = user.getUserId(); // ✅ ID 추출 방식 통일
        log.info("REST API - 세션 종료: userId={}, sessionId={}", userId, sessionId);

        chatbotService.closeSession(sessionId, userId);
    }

    /**
     * 세션 메시지 히스토리 조회 (REST)
     * <p>
     * GET /api/chatbot/sessions/{sessionId}/messages
     * Authorization: Bearer {JWT_TOKEN}
     */
    @GetMapping("/api/chatbot/sessions/{sessionId}/messages")
    @ResponseBody
    public List<ChatResponse> getSessionMessages(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthUser user) { // ✅ Principal -> AuthUser로 수정

        Long userId = user.getUserId(); // ✅ ID 추출 방식 통일
        return chatbotService.getSessionMessages(sessionId, userId);
    }

    // ===== Private Helper Methods =====

    /**
     * 헤더에서 사용자 ID 추출 (WebSocket용)
     */
    private Long getUserIdFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();

        if (principal != null) {
            // JWT 토큰에서 userId 추출
            // JwtAuthenticationFilter에서 설정한 userId 사용
            return ((AuthUser) ((JwtAuthenticationToken) principal).getPrincipal()).getUserId();
        }

        // 테스트용: 헤더에서 직접 가져오기
        String userIdHeader = (String) headerAccessor.getSessionAttributes().get("userId");
        if (userIdHeader != null) {
            return Long.parseLong(userIdHeader);
        }

        throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
    }

    // ===== Inner Classes =====

    /**
     * Typing 이벤트
     */
    private record TypingEvent(Long sessionId, Long userId, boolean isTyping) {
    }
}