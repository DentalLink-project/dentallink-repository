package com.dentallink.domain.chatbot.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.chatbot.entity.ChatMessage;
import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import com.dentallink.domain.chatbot.exception.ChatbotErrorCode;
import com.dentallink.domain.chatbot.repository.ChatMessageRepository;
import com.dentallink.domain.chatbot.repository.ChatSessionRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 상담원 서비스
 * - 상담원 매칭 및 관리
 * - 대기열 관리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConsultantService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    // 대기 큐 (실제 프로덕션에서는 Redis 등 사용)
    private final Queue<WaitingSession> waitingQueue = new ConcurrentLinkedQueue<>();

    /**
     * 상담원에게 전환
     */
    @Transactional
    public ConsultantMatchResult transferToConsultant(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        // 이미 상담원 모드면 리턴
        if (session.isConsultantMode()) {
            return new ConsultantMatchResult(true, session.getConsultant().getId(), 0L);
        }

        // 대기 중인 상담원 찾기
        Optional<User> availableConsultant = findAvailableConsultant();

        if (availableConsultant.isPresent()) {
            // 즉시 상담원 연결
            User consultant = availableConsultant.get();
            session.transferToConsultant(consultant);

            ChatMessage systemMessage = ChatMessage.createSystemMessage(
                    session,
                    String.format("상담원 %s님이 연결되었습니다.", consultant.getUsername())
            );
            messageRepository.save(systemMessage);

            log.info("상담원 즉시 연결: sessionId={}, consultantId={}", sessionId, consultant.getId());
            return new ConsultantMatchResult(true, consultant.getId(), 0L);
        }

        // 대기열에 추가
        WaitingSession waitingSession = new WaitingSession(sessionId, userId);
        waitingQueue.offer(waitingSession);

        long position = waitingQueue.size();

        session.moveToWaitingPosition(position);

        log.info("상담원 대기열 추가: sessionId={}, position={}", sessionId, position);
        return new ConsultantMatchResult(false, null, position);
    }

    /**
     * 상담원 메시지 전송
     */
    @Transactional
    public void sendConsultantMessage(Long sessionId, Long consultantId, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        if (!session.isConsultantMode()) {
            throw new GlobalException(ChatbotErrorCode.NOT_CONSULTANT_SESSION);
        }

        if (!session.getConsultant().getId().equals(consultantId)) {
            throw new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
        }

        ChatMessage message = ChatMessage.createConsultantMessage(session, content);
        messageRepository.save(message);
    }

    /**
     * 상담원이 담당 중인 세션 목록 조회
     */
    public List<ChatSession> getConsultantSessions(Long consultantId) {
        return sessionRepository.findByConsultantIdAndStatus(consultantId, SessionStatus.ACTIVE);
    }

    /**
     * 상담원 세션 종료
     */
    @Transactional
    public void closeConsultantSession(Long sessionId, Long consultantId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        if (!session.getConsultant().getId().equals(consultantId)) {
            throw new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
        }

        session.close();

        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "상담원이 상담을 종료했습니다. 이용해 주셔서 감사합니다."
        );
        messageRepository.save(systemMessage);
    }

    /**
     * 상담원이 대기 중인 세션 가져오기
     */
    @Transactional
    public Optional<ChatSession> pickNextWaitingSession(Long consultantId) {
        WaitingSession waitingSession = waitingQueue.poll();

        if (waitingSession == null) {
            return Optional.empty();
        }

        ChatSession session = sessionRepository.findById(waitingSession.sessionId())
                .orElse(null);

        if (session == null || session.getStatus() != SessionStatus.WAITING) {
            // 유효하지 않은 세션이면 다음 것 시도
            return pickNextWaitingSession(consultantId);
        }

        // 상담원 연결
        User consultant = userRepository.findById(consultantId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.CONSULTANT_NOT_FOUND));

        session.transferToConsultant(consultant);

        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                String.format("상담원 %s님이 연결되었습니다.", consultant.getUsername())
        );
        messageRepository.save(systemMessage);

        // 대기 순번 업데이트 (남은 세션들)
        updateWaitingPositions();

        log.info("상담원이 대기 세션 가져옴: sessionId={}, consultantId={}", session.getId(), consultantId);
        return Optional.of(session);
    }

    /**
     * 대기열 상태 조회
     */
    public QueueStatus getQueueStatus() {
        int waitingCount = waitingQueue.size();
        int activeConsultants = countActiveConsultants();

        return new QueueStatus(waitingCount, activeConsultants);
    }

    // ===== Private Helper Methods =====

    /**
     * 대기 중인 상담원 찾기
     */
    private Optional<User> findAvailableConsultant() {
        // ROLE_ADMIN이거나 특정 상담원 역할을 가진 사용자 중
        // 현재 활성 세션이 3개 미만인 상담원 찾기
        List<User> consultants = userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == UserRole.ROLE_ADMIN)
                .toList();

        for (User consultant : consultants) {
            int activeSessions = sessionRepository
                    .findByConsultantIdAndStatus(consultant.getId(), SessionStatus.ACTIVE)
                    .size();

            if (activeSessions < 3) {  // 상담원당 최대 3개 세션
                return Optional.of(consultant);
            }
        }

        return Optional.empty();
    }

    /**
     * 활성 상담원 수 카운트
     */
    private int countActiveConsultants() {
        return (int) userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == UserRole.ROLE_ADMIN)
                .count();
    }

    /**
     * 대기 순번 업데이트
     */
    private void updateWaitingPositions() {
        Long position = 1L;
        for (WaitingSession ws : waitingQueue) {
            sessionRepository.findById(ws.sessionId()).ifPresent(session -> {
                session.updateWaitingPosition(position);
            });
        }
    }

    // ===== Inner Classes =====

    /**
     * 대기 중인 세션 정보
     */
    private record WaitingSession(Long sessionId, Long userId) {}

    /**
     * 상담원 매칭 결과
     */
    public record ConsultantMatchResult(
            boolean connected,
            Long consultantId,
            Long waitingPosition
    ) {}

    /**
     * 대기열 상태
     */
    public record QueueStatus(
            int waitingCount,
            int activeConsultants
    ) {}
}