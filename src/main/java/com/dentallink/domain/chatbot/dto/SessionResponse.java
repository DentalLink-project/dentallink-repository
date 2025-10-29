package com.dentallink.domain.chatbot.dto;

import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.enums.ChatMode;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessionResponse(
        Long sessionId,
        Long userId,
        ChatMode mode,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long consultantId,
        String consultantName,
        Long waitingPosition,
        int messageCount
) {
    public static SessionResponse from(ChatSession session) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUser().getId())
                .mode(session.getMode())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .consultantId(session.getConsultant() != null ? session.getConsultant().getId() : null)
                .consultantName(session.getConsultant() != null ? session.getConsultant().getUsername() : null)
                .waitingPosition(session.getWaitingPosition())
                .messageCount(session.getMessages().size())
                .build();
    }
}
