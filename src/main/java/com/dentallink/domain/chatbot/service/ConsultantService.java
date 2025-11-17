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
     * 상담원이 특정 세션을 수락 (선택한 세션)
     * @param sessionId 상담원이 선택한 세션 ID
     * @param consultantId 상담원 ID
     * @return 수락된 세션
     */
    @Transactional
    public Optional<ChatSession> pickSpecificSession(Long sessionId, Long consultantId) {
        // DB에서 세션 조회
        Optional<ChatSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            log.warn("세션을 찾을 수 없음: sessionId={}", sessionId);
            // Redis에서도 제거
            redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());
            redisTemplate.delete(SESSION_POSITION_KEY + sessionId);
            return Optional.empty();
        }

        ChatSession session = sessionOpt.get();

        // 세션이 WAITING 상태인지 확인
        if (session.getStatus() != SessionStatus.WAITING) {
            log.warn("세션이 대기 상태가 아님: sessionId={}, status={}", sessionId, session.getStatus());

            // 상태가 WAITING이 아니면 Redis에서 정리 (DB와 Redis 동기화)
            redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());
            redisTemplate.delete(SESSION_POSITION_KEY + sessionId);

            // 대기 순번 업데이트
            updateWaitingPositions();

            return Optional.empty();
        }

        try {
            // 상담원 조회
            User consultant = userRepository.findById(consultantId)
                    .orElseThrow(() -> new GlobalException(ChatbotErrorCode.CONSULTANT_NOT_FOUND));

            // 상담원 연결
            session.transferToConsultant(consultant);

            // Hibernate lazy loading 방지: WebSocket 핸들러에서 비동기로 접근할 수 있으므로
            // 트랜잭션 내에서 명시적으로 로드
            String consultantName = consultant.getUsername();
            Long userId = session.getUser().getId();
            String username = session.getUser().getUsername();

            ChatMessage systemMessage = ChatMessage.createSystemMessage(
                    session,
                    String.format("상담원 %s님이 연결되었습니다.", consultantName)
            );
            messageRepository.save(systemMessage);

            // Redis 대기열에서 제거
            redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());

            // 세션 위치 정보 삭제
            redisTemplate.delete(SESSION_POSITION_KEY + sessionId);

            // 대기 순번 업데이트 (남은 세션들)
            updateWaitingPositions();

            log.info("상담원이 특정 세션을 수락: sessionId={}, consultantId={}, consultantName={}, userId={}",
                    sessionId, consultantId, consultantName, userId);
            return Optional.of(session);
        } catch (Exception e) {
            log.error("세션 수락 중 오류 발생: sessionId={}, consultantId={}", sessionId, consultantId, e);
            return Optional.empty();
        }
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
     * Redis와 DB 동기화: 더 이상 WAITING 상태가 아닌 세션은 Redis에서 정리
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

            if (session.isPresent()) {
                // WAITING 상태인 세션만 리스트에 추가
                if (session.get().getStatus() == SessionStatus.WAITING) {
                    waitingSessions.add(WaitingSessionInfo.from(session.get(), position));
                    position++;
                } else {
                    // WAITING이 아니면 Redis에서 정리 (DB와 Redis 동기화)
                    log.debug("세션이 대기 상태가 아니므로 Redis에서 제거: sessionId={}, status={}",
                            sessionId, session.get().getStatus());
                    redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());
                    redisTemplate.delete(SESSION_POSITION_KEY + sessionId);
                }
            } else {
                // DB에 없는 세션은 Redis에서 정리
                log.debug("세션을 찾을 수 없음: sessionId={}", sessionId);
                redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());
                redisTemplate.delete(SESSION_POSITION_KEY + sessionId);
            }
        }

        return waitingSessions;
    }

    /**
     * 세션의 사용자 ID 조회
     * @param sessionId 세션 ID
     * @return 세션의 사용자 ID
     */
    public Long getUserIdBySessionId(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));
        return session.getUser().getId();
    }

    /**
     * 세션의 메시지 조회
     * @param sessionId 세션 ID
     * @return 세션의 모든 메시지
     */
    public List<ChatSessionMessageDto> getSessionMessages(Long sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);

        if (messages.isEmpty()) {
            log.debug("세션의 메시지가 없음: sessionId={}", sessionId);
            return List.of();
        }

        // 세션 정보 로드 (sender name을 위해)
        ChatSession session = messages.get(0).getSession();

        return messages.stream()
                .map(msg -> {
                    String senderName = determineSenderName(msg, session);
                    return new ChatSessionMessageDto(
                            msg.getId(),
                            msg.getType().toString(),
                            msg.getContent(),
                            senderName,
                            msg.getSentAt()
                    );
                })
                .toList();
    }

    /**
     * 메시지의 발신자 이름 결정
     */
    private String determineSenderName(ChatMessage msg, ChatSession session) {
        return switch (msg.getType()) {
            case USER -> session.getUser().getUsername();
            case CONSULTANT -> session.getConsultant() != null ?
                    session.getConsultant().getUsername() : "상담원";
            case AI -> "AI";
            case SYSTEM -> "시스템";
        };
    }

    /**
     * 세션 메시지 응답 DTO
     */
    public record ChatSessionMessageDto(
            Long id,
            String type,
            String content,
            String senderName,
            java.time.LocalDateTime sentAt
    ) {}

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