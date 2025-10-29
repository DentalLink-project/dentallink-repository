package com.dentallink.domain.chatbot.dto;

import com.dentallink.domain.chatbot.entity.ChatMessage;
import com.dentallink.domain.chatbot.enums.MessageType;import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatResponse (
        Long messageId,
        Long sessionId,
        MessageType type,
        String content,
        LocalDateTime sentAt,

        String functionName,
        String functionResult,

        ActionType actionType,

        Long waitingPosition
){
    public enum ActionType {
        NONE,                    // 일반 메시지
        TRANSFER_TO_CONSULTANT,  // 상담원 전환
        SESSION_CLOSED           // 세션 종료
    }

    public static ChatResponse from(ChatMessage message) {
        return ChatResponse.builder()
                .messageId(message.getId())
                .sessionId(message.getSession().getId())
                .type(message.getType())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .functionName(message.getFunctionName())
                .functionResult(message.getFunctionResult())
                .actionType(ActionType.NONE)
                .build();
    }

    public static ChatResponse createTransferResponse(Long sessionId, Long waitingPosition) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(MessageType.SYSTEM)
                .content(waitingPosition == 0
                        ? "상담원을 연결하고 있습니다..."
                        : String.format("현재 대기 순번: %d번", waitingPosition))
                .sentAt(LocalDateTime.now())
                .actionType(ActionType.TRANSFER_TO_CONSULTANT)
                .waitingPosition(waitingPosition)
                .build();
    }

    public static ChatResponse createSessionClosedResponse(Long sessionId) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .type(MessageType.SYSTEM)
                .content("상담이 종료되었습니다. 이용해 주셔서 감사합니다.")
                .sentAt(LocalDateTime.now())
                .actionType(ActionType.SESSION_CLOSED)
                .build();
    }

}
