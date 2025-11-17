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
            Long consultantId = session.getConsultant() != null ? session.getConsultant().getId() : null;
            return new ConsultantMatchResult(true, consultantId, 0L);
        }

        //개선: 세션이 이미 CLOSED 상태면 새 세션 필요
        if (session.getStatus() == SessionStatus.CLOSED) {
            log.warn("종료된 세션에 상담원 연결 시도: sessionId={}", sessionId);
            throw new GlobalException(ChatbotErrorCode.SESSION_ALREADY_CLOSED);
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

        // 세션 위치를 Redis에 저장
        Long position = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        redisTemplate.opsForValue().set(SESSION_POSITION_KEY + sessionId, position != null ? position.toString() : "1");

        //DB의 세션 상태를 WAITING으로 업데이트
        session.moveToWaitingPosition(position);

        log.info("상담원 대기열 추가: sessionId={}, position={}", sessionId, position);
        return new ConsultantMatchResult(false, null, position);
    }

    /**
     *신규: 대기열에서 세션 제거 (세션 종료 시 호출)
     */
    @Transactional
    public void removeFromWaitingQueue(Long sessionId) {
        log.info("대기열에서 세션 제거: sessionId={}", sessionId);

        try {
            // Redis에서 제거
            redisTemplate.opsForList().remove(WAITING_QUEUE_KEY, 1, sessionId.toString());
            redisTemplate.delete(SESSION_POSITION_KEY + sessionId);

            // 대기 순번 업데이트
            updateWaitingPositions();

            log.info("대기열에서 세션 제거 완료: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("대기열에서 세션 제거 중 오류: sessionId={}", sessionId, e);
        }
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
     *개선: 상담원 세션 종료 (DB 상태 및 Redis 동기화)
     */
    @Transactional
    public void closeConsultantSession(Long sessionId, Long consultantId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        if (session.getConsultant() == null || !session.getConsultant().getId().equals(consultantId)) {
            throw new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
        }

        //세션 종료 처리
        session.close();

        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "상담원이 상담을 종료했습니다. 이용해 주셔서 감사합니다."
        );
        messageRepository.save(systemMessage);

        log.info("상담원 세션 종료: sessionId={}, consultantId={}, finalStatus={}",
                sessionId, consultantId, session.getStatus());
    }

    /**
     * 상담원이 특정 세션을 수락
     */
    @Transactional
    public Optional<ChatSession> pickSpecificSession(Long sessionId, Long consultantId) {
        Optional<ChatSession> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            log.warn("세션을 찾을 수 없음: sessionId={}", sessionId);
            // Redis에서도 제거
            removeFromWaitingQueue(sessionId);
            return Optional.empty();
        }

        ChatSession session = sessionOpt.get();

        //개선: 세션이 WAITING 상태인지 확인
        if (session.getStatus() != SessionStatus.WAITING) {
            log.warn("세션이 대기 상태가 아님: sessionId={}, status={}", sessionId, session.getStatus());

            // Redis 동기화
            removeFromWaitingQueue(sessionId);
            return Optional.empty();
        }

        try {
            // 상담원 조회
            User consultant = userRepository.findById(consultantId)
                    .orElseThrow(() -> new GlobalException(ChatbotErrorCode.CONSULTANT_NOT_FOUND));

            //상담원 연결
            session.transferToConsultant(consultant);

            // Lazy loading 방지
            String consultantName = consultant.getUsername();
            Long userId = session.getUser().getId();
            String username = session.getUser().getUsername();

            ChatMessage systemMessage = ChatMessage.createSystemMessage(
                    session,
                    String.format("상담원 %s님이 연결되었습니다.", consultantName)
            );
            messageRepository.save(systemMessage);

            //Redis 대기열에서 제거
            removeFromWaitingQueue(sessionId);

            log.info("상담원이 특정 세션을 수락: sessionId={}, consultantId={}, consultantName={}, userId={}",
                    sessionId, consultantId, consultantName, userId);
            return Optional.of(session);
        } catch (Exception e) {
            log.error("세션 수락 중 오류 발생: sessionId={}, consultantId={}", sessionId, consultantId, e);
            return Optional.empty();
        }
    }

    /**
     * 상담원이 대기 중인 세션 가져오기 (다음 대기 세션)
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

            //유효한 세션 확인
            if (session != null && session.getStatus() == SessionStatus.WAITING) {
                // 상담원 조회
                User consultant = userRepository.findById(consultantId)
                        .orElseThrow(() -> new GlobalException(ChatbotErrorCode.CONSULTANT_NOT_FOUND));

                // 상담원 연결
                session.transferToConsultant(consultant);

                // Lazy loading 방지
                String consultantName = consultant.getUsername();

                ChatMessage systemMessage = ChatMessage.createSystemMessage(
                        session,
                        String.format("상담원 %s님이 연결되었습니다.", consultantName)
                );
                messageRepository.save(systemMessage);

                // 세션 위치 정보 삭제
                redisTemplate.delete(SESSION_POSITION_KEY + sessionId);

                // 대기 순번 업데이트
                updateWaitingPositions();

                log.info("상담원이 대기 세션 가져옴: sessionId={}, consultantId={}", session.getId(), consultantId);
                return Optional.of(session);
            } else {
                // 유효하지 않은 세션이면 위치 정보 삭제 후 다음 반복
                redisTemplate.delete(SESSION_POSITION_KEY + sessionId);
                log.warn("유효하지 않은 세션 스킵: sessionId={}", sessionId);
            }
        }
    }

    /**
     * 대기열 상태 조회
     */
    public QueueStatus getQueueStatus() {
        Long waitingCount = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        int activeConsultants = countActiveConsultants();

        return new QueueStatus(waitingCount != null ? waitingCount.intValue() : 0, activeConsultants);
    }

    /**
     *개선: 대기 중인 세션 목록 조회 (Redis-DB 동기화 강화)
     */
    public List<WaitingSessionInfo> getWaitingSessions() {
        List<Object> sessionIds = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        List<WaitingSessionInfo> waitingSessions = new ArrayList<>();
        List<Long> invalidSessionIds = new ArrayList<>();
        long position = 1;

        for (Object sessionIdObj : sessionIds) {
            Long sessionId = Long.parseLong(sessionIdObj.toString());
            Optional<ChatSession> session = sessionRepository.findById(sessionId);

            if (session.isPresent()) {
                ChatSession chatSession = session.get();

                //WAITING 상태인 세션만 리스트에 추가
                if (chatSession.getStatus() == SessionStatus.WAITING) {
                    waitingSessions.add(WaitingSessionInfo.from(chatSession, position));
                    position++;
                } else {
                    // WAITING이 아니면 정리 대상
                    log.debug("세션이 대기 상태가 아니므로 정리: sessionId={}, status={}",
                            sessionId, chatSession.getStatus());
                    invalidSessionIds.add(sessionId);
                }
            } else {
                // DB에 없는 세션은 정리 대상
                log.debug("세션을 찾을 수 없음: sessionId={}", sessionId);
                invalidSessionIds.add(sessionId);
            }
        }

        //일괄 정리 (Redis 명령 최소화)
        if (!invalidSessionIds.isEmpty()) {
            for (Long sessionId : invalidSessionIds) {
                removeFromWaitingQueue(sessionId);
            }
        }

        return waitingSessions;
    }

    /**
     * 세션의 사용자 ID 조회
     */
    public Long getUserIdBySessionId(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));
        return session.getUser().getId();
    }

    /**
     * 세션의 메시지 조회
     */
    public List<ChatSessionMessageDto> getSessionMessages(Long sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);

        if (messages.isEmpty()) {
            log.debug("세션의 메시지가 없음: sessionId={}", sessionId);
            return List.of();
        }

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

    // ===== Private Helper Methods =====

    /**
     * 대기 중인 상담원 찾기
     */
    private Optional<User> findAvailableConsultant() {
        // ROLE_ADMIN인 사용자 중 활성 세션이 3개 미만인 상담원 찾기
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
     * 개선: 대기 순번 업데이트 (Redis 기반)
     */
    private void updateWaitingPositions() {
        Long size = redisTemplate.opsForList().size(WAITING_QUEUE_KEY);
        if (size == null || size == 0) return;

        List<Object> sessionIds = redisTemplate.opsForList().range(WAITING_QUEUE_KEY, 0, -1);
        if (sessionIds == null) return;

        // Redis에서만 순번 업데이트
        long position = 1;
        for (Object sessionIdObj : sessionIds) {
            Long sessionId = Long.parseLong(sessionIdObj.toString());
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
}