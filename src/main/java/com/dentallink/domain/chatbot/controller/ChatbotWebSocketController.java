package com.dentallink.domain.chatbot.controller;

import com.dentallink.domain.chatbot.dto.ChatRequest;
import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.dto.SessionResponse;
import com.dentallink.domain.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 챗봇 WebSocket Controller
 * - 실시간 채팅 메시지 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatbotWebSocketController {

    private final ChatbotService chatbotService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 전송
     * 클라이언트: /app/chat/send
     * 응답: /user/queue/reply
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload @Valid ChatRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // 사용자 ID 가져오기 (인증된 사용자)
        Long userId = getUserIdFromHeader(headerAccessor);

        log.info("채팅 메시지 수신: userId={}, sessionId={}, content={}",
                userId, request.sessionId(), request.content());

        try {
            // AI 응답 생성
            ChatResponse response = chatbotService.processMessage(request, userId);

            // 사용자에게 응답 전송
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/reply",
                    response
            );

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);

            // 에러 응답 전송
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
     * 세션 종료
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

    // ===== REST API (세션 정보 조회용) =====

    /**
     * 내 활성 세션 조회 (REST)
     */
    @GetMapping("/api/chatbot/sessions/active")
    @ResponseBody
    public SessionResponse getActiveSession(@RequestParam Long userId) {
        // 실제로는 Spring Security에서 userId를 가져와야 함
        return chatbotService.getMySessions(userId,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 세션 메시지 히스토리 조회 (REST)
     */
    @GetMapping("/api/chatbot/sessions/{sessionId}/messages")
    @ResponseBody
    public List<ChatResponse> getSessionMessages(
            @PathVariable Long sessionId,
            @RequestParam Long userId) {

        return chatbotService.getSessionMessages(sessionId, userId);
    }

    // ===== Private Helper Methods =====

    /**
     * 헤더에서 사용자 ID 추출
     * 실제로는 Spring Security Principal에서 가져와야 함
     */
    private Long getUserIdFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        // Principal을 통해 인증된 사용자 ID 가져오기
        Principal principal = headerAccessor.getUser();

        if (principal != null) {
            // JWT 토큰이나 세션에서 userId 추출
            // 예: return Long.parseLong(principal.getName());
            return Long.parseLong(principal.getName());
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
    private record TypingEvent(Long sessionId, Long userId, boolean isTyping) {}
}