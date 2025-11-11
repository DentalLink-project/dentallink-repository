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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 상수
    private static final String WAITING_QUEUE_KEY = "chatbot:waiting:queue";
    private static final String SESSION_POSITION_KEY = "chatbot:session:position:";
    private static final String ACTIVE_CONSULTANTS_KEY = "chatbot:active:consultants:";

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

        // Redis 대기열에 추가
        redisTemplate.opsForList().rightPush(WAITING_QUEUE_KEY, sessionId.toString());

        // 세션 위치를 Redis에만 저장 (String으로 변환 - StringRedisSerializer 호환)
        Long position = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        redisTemplate.opsForValue().set(SESSION_POSITION_KEY + sessionId, position != null ? position.toString() : "1");

        // DB의 세션 상태를 WAITING으로 업데이트 (필수: getWaitingSessions에서 필터링)
        session.moveToWaitingPosition(position);

        log.info("상담원 대기열 추가 (Redis): sessionId={}, position={}", sessionId, position);
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
     * 상담원이 대기 중인 세션 가져오기 (Redis 기반)
     * 반복문을 사용하여 유효한 세션을 찾음 (재귀로 인한 StackOverflowError 방지)
     */
    @Transactional
    public Optional<ChatSession> pickNextWaitingSession(Long consultantId) {
        // 유효한 세션을 찾을 때까지 반복
        while (true) {
            // Redis에서 대기열의 첫 번째 sessionId 가져오기
            Object sessionIdObj = redisTemplate.opsForList().leftPop(WAITING_QUEUE_KEY);

            if (sessionIdObj == null) {
                // 대기열이 비었음
                return Optional.empty();
            }

            Long sessionId = Long.parseLong(sessionIdObj.toString());
            ChatSession session = sessionRepository.findById(sessionId).orElse(null);

            // 유효한 세션 확인
            if (session != null && session.getStatus() == SessionStatus.WAITING) {
                // 상담원 연결
                User consultant = userRepository.findById(consultantId)
                        .orElseThrow(() -> new GlobalException(ChatbotErrorCode.CONSULTANT_NOT_FOUND));

                session.transferToConsultant(consultant);

                ChatMessage systemMessage = ChatMessage.createSystemMessage(
                        session,
                        String.format("상담원 %s님이 연결되었습니다.", consultant.getUsername())
                );
                messageRepository.save(systemMessage);

                // 세션 위치 정보 삭제
                redisTemplate.delete(SESSION_POSITION_KEY + sessionId);

                // 대기 순번 업데이트 (남은 세션들)
                updateWaitingPositions();

                log.info("상담원이 대기 세션 가져옴 (Redis): sessionId={}, consultantId={}", session.getId(), consultantId);
                return Optional.of(session);
            } else {
                // 유효하지 않은 세션이면 위치 정보 삭제 후 다음 반복으로
                redisTemplate.delete(SESSION_POSITION_KEY + sessionId);
                log.warn("유효하지 않은 세션 스킵: sessionId={}", sessionId);
                // while 루프 계속
            }
        }
    }

    /**
     * 대기열 상태 조회 (Redis 기반)
     */
    public QueueStatus getQueueStatus() {
        Long waitingCount = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        int activeConsultants = countActiveConsultants();

        return new QueueStatus(waitingCount != null ? waitingCount.intValue() : 0, activeConsultants);
    }

    /**
     * 대기 중인 세션 목록 조회 (Redis 기반)
     */
    public List<WaitingSessionInfo> getWaitingSessions() {
        List<Object> sessionIds = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        List<WaitingSessionInfo> waitingSessions = new ArrayList<>();
        long position = 1;

        for (Object sessionIdObj : sessionIds) {
            Long sessionId = Long.parseLong(sessionIdObj.toString());
            Optional<ChatSession> session = sessionRepository.findById(sessionId);

            if (session.isPresent() && session.get().getStatus() == SessionStatus.WAITING) {
                waitingSessions.add(WaitingSessionInfo.from(session.get(), position));
            }
            position++;
        }

        return waitingSessions;
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
     * 대위 순번 업데이트 (Redis 기반, DB 쿼리 최소화)
     * N+1 쿼리 문제를 해결하기 위해 Redis에서만 순번 관리
     * DB의 waitingPosition은 참고용이며, Redis가 단일 소스
     */
    private void updateWaitingPositions() {
        Long size = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        if (size == null || size == 0) return;

        // Redis의 모든 sessionId 조회
        List<Object> sessionIds = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);
        if (sessionIds == null) return;

        // Redis에서만 순번 업데이트 (O(N) 복잡도, DB 쿼리 없음)
        long position = 1;
        for (Object sessionIdObj : sessionIds) {
            Long sessionId = Long.parseLong(sessionIdObj.toString());
            // Redis에만 저장 - String으로 변환 (StringRedisSerializer 호환)
            redisTemplate.opsForValue().set(SESSION_POSITION_KEY + sessionId, String.valueOf(position));
            position++;
        }

        log.debug("대기 순번 업데이트 완료: 총 {} 개 세션", size);
    }

    // ===== Inner Classes (Records) =====

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

    /**
     * 대기 중인 세션 정보
     */
    public record WaitingSessionInfo(
            Long sessionId,
            Long userId,
            String username,
            Long waitingPosition,
            java.time.LocalDateTime startedAt
    ) {
        public static WaitingSessionInfo from(ChatSession session, long position) {
            return new WaitingSessionInfo(
                    session.getId(),
                    session.getUser().getId(),
                    session.getUser().getUsername(),
                    position,
                    session.getStartedAt()
            );
        }
    }
}