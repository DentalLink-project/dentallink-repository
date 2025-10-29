package com.dentallink.domain.chatbot.entity;

import com.dentallink.domain.chatbot.enums.ChatMode;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import com.dentallink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id")
    private User consultant;

    private Long waitingPosition;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static ChatSession startAISession(User user) {
        ChatSession ssession = new ChatSession();
        ssession.user = user;
        ssession.mode = ChatMode.AI;
        ssession.status = SessionStatus.ACTIVE;
        ssession.startedAt = LocalDateTime.now();
        return ssession;
    }

    public void transferToConsultant(User consultant) {
        this.mode = ChatMode.CONSULTANT;
        this.consultant = consultant;
        this.status = SessionStatus.ACTIVE;
        this.waitingPosition = null;
    }

    public void moveToWaitingPosition(Long position) {
        this.status = SessionStatus.WAITING;
        this.waitingPosition = position;
    }

    public void updateWaitingPosition(Long position) {
        this.waitingPosition = position;
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
        this.endedAt = LocalDateTime.now();
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isActive() {
        return this.status == SessionStatus.ACTIVE;
    }

    public boolean isConsultantMode() {
        return this.mode == ChatMode.CONSULTANT;
    }
}
