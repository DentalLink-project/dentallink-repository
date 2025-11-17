package com.dentallink.domain.chatbot.repository;

import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByUserIdAndStatus(Long userId, SessionStatus staus, Pageable pageable);

    Page<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    List<ChatSession> findByConsultantIdAndStatus(Long consultantId, SessionStatus status);
}
