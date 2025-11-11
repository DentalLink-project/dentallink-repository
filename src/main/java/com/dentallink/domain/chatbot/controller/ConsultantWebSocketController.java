package com.dentallink.domain.chatbot.controller;

import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.service.ConsultantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 상담원 WebSocket Controller
 * - 상담원과 사용자 간 실시간 메시지 중계
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConsultantWebSocketController {

    private final ConsultantService consultantService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 상담원 메시지 전송
     * 클라이언트: /app/consultant/send
     */
    @MessageMapping("/consultant/send")
    public void sendConsultantMessage(
            @Payload ConsultantMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Long consultantId = getConsultantIdFromHeader(headerAccessor);

        log.info("상담원 메시지 전송: consultantId={}, sessionId={}",
                consultantId, request.sessionId());

        try {
            // 메시지 저장
            consultantService.sendConsultantMessage(
                    request.sessionId(),
                    consultantId,
                    request.content()
            );

            // 사용자에게 메시지 전송
            ChatResponse response = ChatResponse.builder()
                    .sessionId(request.sessionId())
                    .type(com.dentallink.domain.chatbot.enums.MessageType.CONSULTANT)
                    .content(request.content())
                    .sentAt(java.time.LocalDateTime.now())
                    .build();

            // 해당 세션의 사용자에게 전송
            messagingTemplate.convertAndSendToUser(
                    request.userId().toString(),
                    "/queue/reply",
                    response
            );

        } catch (Exception e) {
            log.error("상담원 메시지 전송 중 오류 발생", e);
        }
    }

    /**
     * 특정 대기 세션 수락
     * 클라이언트: /app/consultant/pick
     * 상담원이 선택한 특정 세션을 수락함
     */
    @MessageMapping("/consultant/pick")
    public void pickNextSession(
            @Payload PickSessionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        Long consultantId = getConsultantIdFromHeader(headerAccessor);

        log.info("상담원이 세션 수락 요청: consultantId={}, sessionId={}", consultantId, request.sessionId());

        try {
            // 상담원이 선택한 특정 세션 수락
            Optional<ChatSession> session = consultantService.pickSpecificSession(request.sessionId(), consultantId);

            if (session.isPresent()) {
                // 상담원에게 새 세션 알림
                SessionAssignedEvent event = new SessionAssignedEvent(
                        session.get().getId(),
                        session.get().getUser().getId(),
                        session.get().getUser().getUsername()
                );

                messagingTemplate.convertAndSendToUser(
                        consultantId.toString(),
                        "/queue/assigned",
                        event
                );

                // 사용자에게 상담원 연결 알림
                ChatResponse response = ChatResponse.builder()
                        .sessionId(session.get().getId())
                        .type(com.dentallink.domain.chatbot.enums.MessageType.SYSTEM)
                        .content(String.format("상담원 %s님이 연결되었습니다.",
                                session.get().getConsultant().getUsername()))
                        .sentAt(java.time.LocalDateTime.now())
                        .build();

                messagingTemplate.convertAndSendToUser(
                        session.get().getUser().getId().toString(),
                        "/queue/reply",
                        response
                );

            } else {
                // 세션 수락 실패 (세션을 찾을 수 없음 또는 이미 할당됨)
                log.warn("세션 수락 실패: sessionId={}", request.sessionId());
                messagingTemplate.convertAndSendToUser(
                        consultantId.toString(),
                        "/queue/assigned",
                        new SessionPickFailedEvent("세션을 찾을 수 없거나 이미 할당되었습니다.")
                );
            }

        } catch (Exception e) {
            log.error("세션 할당 중 오류 발생", e);
            messagingTemplate.convertAndSendToUser(
                    consultantId.toString(),
                    "/queue/assigned",
                    new SessionPickFailedEvent("세션 수락 중 오류가 발생했습니다.")
            );
        }
    }

    /**
     * 상담원 세션 종료
     * 클라이언트: /app/consultant/close
     */
    @MessageMapping("/consultant/close")
    public void closeSession(
            @Payload Long sessionId,
            SimpMessageHeaderAccessor headerAccessor) {

        Long consultantId = getConsultantIdFromHeader(headerAccessor);

        log.info("상담원 세션 종료: consultantId={}, sessionId={}", consultantId, sessionId);

        try {
            consultantService.closeConsultantSession(sessionId, consultantId);

            // 상담원에게 종료 확인
            messagingTemplate.convertAndSendToUser(
                    consultantId.toString(),
                    "/queue/closed",
                    new SessionClosedEvent(sessionId)
            );

        } catch (Exception e) {
            log.error("세션 종료 중 오류 발생", e);
        }
    }

    // ===== REST API =====

    /**
     * 상담원 담당 세션 목록 조회
     */
    @GetMapping("/api/consultant/sessions")
    @ResponseBody
    public List<ChatSession> getConsultantSessions(@RequestParam Long consultantId) {
        return consultantService.getConsultantSessions(consultantId);
    }

    /**
     * 대기열 상태 조회
     */
    @GetMapping("/api/consultant/queue/status")
    @ResponseBody
    public ConsultantService.QueueStatus getQueueStatus() {
        return consultantService.getQueueStatus();
    }

    /**
     * 대기 중인 세션 목록 조회
     */
    @GetMapping("/api/consultant/waiting-sessions")
    @ResponseBody
    public List<ConsultantService.WaitingSessionInfo> getWaitingSessions() {
        return consultantService.getWaitingSessions();
    }

    // ===== Private Helper Methods =====

    private Long getConsultantIdFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        // Principal에서 상담원 ID 추출
        var principal = headerAccessor.getUser();
        if (principal != null) {
            return Long.parseLong(principal.getName());
        }

        // 테스트용 - 타입 안전성 강화
        Object consultantIdObj = headerAccessor.getSessionAttributes().get("consultantId");
        if (consultantIdObj != null) {
            // 다양한 타입 처리 (String, Long, Number 등)
            if (consultantIdObj instanceof String) {
                return Long.parseLong((String) consultantIdObj);
            } else if (consultantIdObj instanceof Long) {
                return (Long) consultantIdObj;
            } else if (consultantIdObj instanceof Number) {
                return ((Number) consultantIdObj).longValue();
            } else {
                return Long.parseLong(consultantIdObj.toString());
            }
        }

        throw new IllegalArgumentException("인증되지 않은 상담원입니다.");
    }

    // ===== Inner Classes =====

    private record PickSessionRequest(
            Long sessionId
    ) {}

    private record ConsultantMessageRequest(
            Long sessionId,
            Long userId,  // 메시지를 받을 사용자 ID
            String content
    ) {}

    private record SessionAssignedEvent(
            Long sessionId,
            Long userId,
            String userName
    ) {}

    private record SessionClosedEvent(Long sessionId) {}

    private record NoSessionAvailableEvent() {}

    private record SessionPickFailedEvent(String message) {}
}