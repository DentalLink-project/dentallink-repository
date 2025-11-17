package com.dentallink.domain.chatbot.entity;

import com.dentallink.domain.chatbot.enums.MessageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_session_sent_at", columnList = "session_id, sent_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private String functionName;

    @Column(columnDefinition = "TEXT")
    private String functionParams;

    @Column(columnDefinition = "TEXT")
    private String functionResult;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    //사용자 메세지 생성
    public static ChatMessage createUserMessage(ChatSession session, String content) {
        ChatMessage message = new ChatMessage();
        message.session = session;
        message.type = MessageType.USER;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    //AI 응답메세지 생성
    public static ChatMessage createAIMessage(ChatSession session, String content) {
        ChatMessage message = new ChatMessage();
        message.session = session;
        message.type = MessageType.AI;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    public static ChatMessage createAIMessageWithFunction(
            ChatSession session,
            String content,
            String functionName,
            String functionParams,
            String functionResult) {
        ChatMessage message = new ChatMessage();
        message.session = session;
        message.type = MessageType.AI;
        message.content = content;
        message.functionName = functionName;
        message.functionParams = functionParams;
        message.functionResult = functionResult;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    public static ChatMessage createConsultantMessage(ChatSession session, String content) {
        ChatMessage message = new ChatMessage();
        message.session = session;
        message.type = MessageType.CONSULTANT;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    public static ChatMessage createSystemMessage(ChatSession session, String content) {
        ChatMessage message = new ChatMessage();
        message.session = session;
        message.type = MessageType.SYSTEM;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        return message;
    }
}
