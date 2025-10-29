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
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByUserIdAndStatus(Long userId, SessionStatus staus);

    Page<ChatSession> findByUserIdAndStatus(Long userId, SessionStatus staus, Pageable pageable);

    Page<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    List<ChatSession> findByConsultantIdAndStatus(Long consultantId, SessionStatus status);

    @Query("SELECT COUNT(s) FROM ChatSession s WHERE s.status = :status")
    int countByStatus(@Param("status") SessionStatus status);

    @Query("SELECT s FROM ChatSession s WHERE s.status = :status ORDER BY s.startedAt ASC")
    List<ChatSession> findWaitingSessions(@Param("status") SessionStatus status);

    @Query("SELECT s FROM ChatSession s WHERE s.startedAt BETWEEN :startDate AND :endDate")
    List<ChatSession> findSessionsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT s FROM ChatSession s " +
            "WHERE s.status = :status " +
            "AND s.updatedAt < :threshold")
    List<ChatSession> findInactiveActiveSessions(
            @Param("status") SessionStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
