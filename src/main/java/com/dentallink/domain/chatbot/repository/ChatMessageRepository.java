package com.dentallink.domain.chatbot.repository;

import com.dentallink.domain.chatbot.entity.ChatMessage;
import com.dentallink.domain.chatbot.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {

    List<ChatMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);

    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.session.id = :sessionId " +
            "ORDER BY m.sentAt DESC " +
            "LIMIT :limit")
    List<ChatMessage> findRecentMessages(
            @Param("sessionId") Long sessionId,
            @Param("limit") int limit
    );
}
