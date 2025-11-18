package com.dentallink.domain.chatbot.service;

import com.dentallink.common.config.GeminiConfig;
import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.chatbot.dto.ChatRequest;
import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.dto.GeminiFunction;
import com.dentallink.domain.chatbot.dto.SessionResponse;
import com.dentallink.domain.chatbot.entity.ChatMessage;
import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.enums.MessageType;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import com.dentallink.domain.chatbot.exception.ChatbotErrorCode;
import com.dentallink.domain.chatbot.repository.ChatMessageRepository;
import com.dentallink.domain.chatbot.repository.ChatSessionRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.exception.UserErrorCode;
import com.dentallink.domain.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GeminiApiService geminiApiService;
    private final FunctionCallHandler functionCallHandler;
    private final ConsultantService consultantService;
    private final GeminiConfig geminiConfig;
    private final SimpMessagingTemplate messagingTemplate;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDate.class,
                    (JsonSerializer<java.time.LocalDate>) (src, typeOfSrc, context) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalTime.class,
                    (JsonSerializer<java.time.LocalTime>) (src, typeOfSrc, context) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .create();

    // 글로벌 Rate Limiter
    private final RateLimiter globalRateLimiter = RateLimiter.create(15.0 / 60.0);
    private final ConcurrentHashMap<Long, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    @Transactional
    public ChatResponse processMessage(ChatRequest request, Long userId) {

        validateMessage(request.content());
        ChatSession session = getOrCreateSession(request.sessionId(), userId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            log.info("종료된 세션 감지, 새 세션 생성: oldSessionId={}, userId={}", session.getId(), userId);
            session = createNewSession(userId);
        }

        // 상담원 모드 처리
        if (session.isConsultantMode()) {
            return handleConsultantMessage(session, request, userId);
        }

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

        // 상담원 연결 키워드 감지
        if (isConsultantRequestKeyword(request.content())) {
            return handleConsultantRequest(session, userId);
        }

        // Rate Limit 체크
        if (!checkRateLimit(userId)) {
            return handleRateLimitExceeded(session, userId);
        }

        try {
            return generateAIResponseOptimized(session, userId, request.content());

        } catch (GlobalException e) {
            if (e.getErrorCode() == ChatbotErrorCode.RATE_LIMIT_EXCEEDED) {
                return handleRateLimitExceeded(session, userId);
            }
            throw e;
        }
    }

    /**
     * 상담원 연결 요청 처리 로직 분리
     */
    private ChatResponse handleConsultantRequest(ChatSession session, Long userId) {
        log.info("상담원 연결 요청 감지: userId={}, sessionId={}", userId, session.getId());

        // 시스템 메시지 저장
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "상담원 연결을 요청하셨습니다. 잠시만 기다려주세요."
        );
        messageRepository.save(systemMessage);

        // 상담원 연결 시도
        var matchResult = consultantService.transferToConsultant(session.getId(), userId);

        if (matchResult.connected()) {
            // 즉시 연결됨
            log.info("상담원 즉시 연결: sessionId={}, consultantId={}", session.getId(), matchResult.consultantId());

            ChatMessage connectedMessage = ChatMessage.createSystemMessage(
                    session,
                    "상담원이 연결되었습니다. 무엇을 도와드릴까요?"
            );
            messageRepository.save(connectedMessage);

            return ChatResponse.builder()
                    .sessionId(session.getId())
                    .type(MessageType.SYSTEM)
                    .content("상담원이 연결되었습니다.")
                    .sentAt(connectedMessage.getSentAt())
                    .build();
        } else {
            // 대기열 추가됨
            log.info("상담원 대기열 추가: sessionId={}, position={}", session.getId(), matchResult.waitingPosition());

            return ChatResponse.createTransferResponse(session.getId(), matchResult.waitingPosition());
        }
    }

    /**
     * AI 응답 생성 (DB 중심 최적화)
     */
    private ChatResponse generateAIResponseOptimized(ChatSession session, Long userId, String currentUserInput) {

        // 1. 대화 히스토리 최소화 (최근 3개만)
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(session.getId(), 3);
        List<GeminiFunction.GeminiMessage> geminiMessages = convertToGeminiMessages(recentMessages);

        // 2. Function 선언 가져오기
        List<GeminiFunction.FunctionDeclaration> functions = functionCallHandler.getFunctionDeclarations();

        // 3. 시스템 프롬프트 추가 (DB 우선 원칙)
        geminiMessages.add(0, createDBFocusedSystemPrompt());

        // 4. 현재 사용자 입력 추가
        if (!geminiMessages.isEmpty()) {
            GeminiFunction.GeminiMessage lastMsg = geminiMessages.get(geminiMessages.size() - 1);
            if (!"user".equals(lastMsg.role())) {
                geminiMessages.add(GeminiFunction.GeminiMessage.builder()
                        .role("user")
                        .content(currentUserInput)
                        .build());
            }
        }

        // 5. Gemini API 호출
        GeminiApiService.GeminiApiResponse geminiResponse =
                geminiApiService.generateContent(geminiMessages, functions);

        // 6. Function Call 우선 처리
        if (geminiResponse.hasFunctionCalls()) {
            return handleFunctionCalls(session, geminiResponse, geminiMessages, functions, userId);
        }

        // 7. 일반 응답
        ChatMessage aiMessage = ChatMessage.createAIMessage(session, geminiResponse.text());
        messageRepository.save(aiMessage);

        return ChatResponse.from(aiMessage);
    }

    /**
     * DB 중심 시스템 프롬프트 (검색 기능 강화)
     */
    private GeminiFunction.GeminiMessage createDBFocusedSystemPrompt() {
        String systemPrompt = """
                당신은 DentalLink 치과 예약 시스템의 AI 상담사입니다.
                
                핵심 원칙: 데이터베이스 우선 답변
                - 사용자 질문에 대해 항상 Function Call을 먼저 고려하세요
                - 추측하지 말고, DB에서 정확한 정보를 조회하세요
                - 대화 맥락보다 현재 질문에 집중하세요
                
                필수 행동 규칙:
                1. 병원 관련 질문 → 즉시 적절한 search_hospitals* 함수 호출
                   - 병원 이름이 주어진 경우: search_hospitals
                   - 지역/위치가 주어진 경우: search_hospitals_by_location
                   - 의사 이름이 주어진 경우: search_hospitals_by_doctor
                
                2. 예약 관련 질문 → get_my_reservations 또는 get_available_times 호출
                
                3. 검색 시 주의사항:
                   - "김해병원", "서울치과" 등 병원명 → search_hospitals
                   - "강남", "김해", "서초동" 등 지역명 → search_hospitals_by_location
                   - "김철수 원장" 등 의사명 → search_hospitals_by_doctor
                   - 검색 결과는 DB에 있는 모든 결과를 보여줘야 함 (5개 제한 없음)
                
                4. 불확실한 정보는 Function Call로 확인 후 답변
                
                하지 말아야 할 것:
                - "이전에 말씀하신 것처럼..." 같은 대화 맥락 언급
                - 추측성 답변 ("아마도...", "~일 것 같습니다")
                - Function Call 없이 병원명이나 예약 정보 언급
                - 검색 결과를 5개로 제한하는 언급
                
                 올바른 응답 예시:
                Q: "강남에 있는 치과 알려줘"
                A: [search_hospitals_by_location("강남") 호출] → DB의 모든 강남 치과 결과 제공
                
                Q: "김해병원 검색해줘"
                A: [search_hospitals("김해병원") 호출] → DB의 모든 김해병원 결과 제공
                
                Q: "내 예약 보여줘"
                A: [get_my_reservations 호출] → 실제 예약 내역 제공
                
                Q: "그 병원 예약 가능한 시간은?"
                A: [병원 ID 확인 → get_available_times 호출] → 정확한 시간대 제공
                
                 제공 가능한 기능:
                - search_hospitals: 병원 이름으로 검색
                - search_hospitals_by_location: 지역/주소로 병원 검색
                - search_hospitals_by_doctor: 의사 이름으로 병원 검색
                - get_available_times: 특정 병원의 예약 가능 시간 조회
                - create_reservation: 예약 생성 (1000P 차감)
                - get_my_reservations: 내 예약 목록 조회
                - cancel_reservation: 예약 취소
                
                 응답 스타일:
                - 간결하고 정확하게 (DB 결과 기반)
                - 모든 검색 결과를 빠짐없이 제공
                - 불필요한 대화 맥락 언급 최소화
                - 한국어 사용
                - 복잡한 요청은 상담원 연결 제안
                """;

        return GeminiFunction.GeminiMessage.builder()
                .role("user")
                .content(systemPrompt)
                .build();
    }

    /**
     * Function Call 처리
     */
    private ChatResponse handleFunctionCalls(
            ChatSession session,
            GeminiApiService.GeminiApiResponse geminiResponse,
            List<GeminiFunction.GeminiMessage> conversationHistory,
            List<GeminiFunction.FunctionDeclaration> functions,
            Long userId) {

        List<GeminiFunction.FunctionResponse> functionResponses = new ArrayList<>();

        // 모든 Function Call 실행
        for (GeminiFunction.FunctionCall fc : geminiResponse.functionCalls()) {
            log.info("Function Call 실행: name={}, args={}", fc.name(), fc.arguments());

            Object result = functionCallHandler.executeFunction(fc, userId);

            functionResponses.add(GeminiFunction.FunctionResponse.builder()
                    .name(fc.name())
                    .response(Map.of("result", result))
                    .build());
        }

        // 히스토리 최소화: 시스템 프롬프트 + Function Call + Function Response만 유지
        List<GeminiFunction.GeminiMessage> simplifiedHistory = new ArrayList<>();
        simplifiedHistory.add(createDBFocusedSystemPrompt());

        // Function Call 추가
        simplifiedHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("model")
                .functionCalls(geminiResponse.functionCalls())
                .build());

        // Function Response 추가
        simplifiedHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("user")
                .functionResponses(functionResponses)
                .build());

        // 최종 응답 생성
        GeminiApiService.GeminiApiResponse finalResponse =
                geminiApiService.generateContent(simplifiedHistory, functions);

        // AI 메시지 저장
        ChatMessage aiMessage = ChatMessage.createAIMessageWithFunction(
                session,
                finalResponse.text(),
                geminiResponse.functionCalls().get(0).name(),
                gson.toJson(geminiResponse.functionCalls().get(0).arguments()),
                gson.toJson(functionResponses)
        );
        messageRepository.save(aiMessage);

        return ChatResponse.from(aiMessage);
    }

    /**
     * 상담원 모드 메시지 처리
     */
    private ChatResponse handleConsultantMessage(ChatSession session, ChatRequest request, Long userId) {
        log.info("상담원 모드 메시지 처리: sessionId={}, userId={}", session.getId(), userId);

        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

        // 상담원에게 메시지 전송
        if (session.getConsultant() != null) {
            messagingTemplate.convertAndSendToUser(
                    session.getConsultant().getId().toString(),
                    "/queue/reply",
                    ChatResponse.from(userMessage)
            );
        }

        // 사용자는 자신의 메시지를 이미 UI에서 표시했으므로 null 반환 (프론트에서 처리하지 않음)
        return null;
    }

    /**
     * Rate Limit 초과 시 상담원 전환
     */
    private ChatResponse handleRateLimitExceeded(ChatSession session, Long userId) {
        log.warn("Rate Limit 초과 - 상담원 전환: userId={}", userId);

        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "요청이 많아 AI 응답이 제한되었습니다. 상담원을 연결해드리겠습니다."
        );
        messageRepository.save(systemMessage);

        var matchResult = consultantService.transferToConsultant(session.getId(), userId);

        return ChatResponse.createTransferResponse(session.getId(), matchResult.waitingPosition());
    }

    /**
     * 개선: 세션 가져오기 또는 생성 (상태 확인 강화)
     */
    private ChatSession getOrCreateSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

            validateSessionAccess(session, userId);

            //CLOSED 세션은 사용 불가
            if (session.getStatus() == SessionStatus.CLOSED) {
                log.info("종료된 세션 접근 시도: sessionId={}", sessionId);
                return createNewSession(userId);
            }

            return session;
        }

        return createNewSession(userId);
    }

    /**
     * 신규: 새 세션 생성 헬퍼 메서드
     */
    private ChatSession createNewSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        ChatSession newSession = ChatSession.startAISession(user);
        return sessionRepository.save(newSession);
    }

    /**
     * 개선: 세션 종료 - 상담원 연결 해제 및 상태 변경
     */
    @Transactional
    public void closeSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        // 종료 전 상태 저장 (session.close() 호출 전에!)
        SessionStatus statusBeforeClose = session.getStatus();
        boolean isConsultantMode = session.isConsultantMode();
        User consultant = session.getConsultant();

        // 세션 종료 처리
        session.close();

        // 상담원 모드였다면 상담원에게 알림
        if (isConsultantMode && consultant != null) {
            log.info("상담원 세션 종료 알림: sessionId={}, consultantId={}",
                    sessionId, consultant.getId());

            try {
                messagingTemplate.convertAndSendToUser(
                        consultant.getId().toString(),
                        "/queue/closed",
                        Map.of("sessionId", sessionId, "reason", "user_closed")
                );
            } catch (Exception e) {
                log.error("상담원 세션 종료 알림 실패: sessionId={}", sessionId, e);
            }
        }

        // WAITING 상태였다면 대기열에서 제거 (종료 전 상태 확인!)
        if (statusBeforeClose == SessionStatus.WAITING) {
            log.info("대기 중인 세션 종료 - 대기열에서 제거: sessionId={}", sessionId);
            consultantService.removeFromWaitingQueue(sessionId);
        }

        log.info("세션 종료 완료: sessionId={}, userId={}, statusBeforeClose={}, finalStatus={}",
                sessionId, userId, statusBeforeClose, session.getStatus());
    }

    /**
     * 세션 조회
     */
    public SessionResponse getSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        return SessionResponse.from(session);
    }

    /**
     * 내 세션 목록 조회
     */
    public Page<SessionResponse> getMySessions(Long userId, Pageable pageable) {
        Page<ChatSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
        return sessions.map(SessionResponse::from);
    }

    /**
     * 개선: 사용자 로그아웃 시 모든 세션 정리 (상담원 알림 포함)
     */
    @Transactional
    public void closeAllSessionsForUser(Long userId) {
        log.info("사용자 로그아웃 - 활성 세션 정리: userId={}", userId);

        // ACTIVE 세션 정리
        Page<ChatSession> activeSessions = sessionRepository.findByUserIdAndStatus(
                userId,
                SessionStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, 1000)
        );

        for (ChatSession session : activeSessions.getContent()) {
            // 종료 전 상담원 정보 저장
            boolean isConsultantMode = session.isConsultantMode();
            User consultant = session.getConsultant();

            // 세션 종료
            session.close();

            // 상담원 모드였다면 알림
            if (isConsultantMode && consultant != null) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            consultant.getId().toString(),
                            "/queue/closed",
                            Map.of("sessionId", session.getId(), "reason", "user_logout")
                    );
                } catch (Exception e) {
                    log.error("상담원 세션 종료 알림 실패: sessionId={}", session.getId(), e);
                }
            }

            log.info("세션 종료: sessionId={}, userId={}", session.getId(), userId);
        }

        sessionRepository.saveAll(activeSessions.getContent());

        // 대기 중인 세션도 정리
        Page<ChatSession> waitingSessions = sessionRepository.findByUserIdAndStatus(
                userId,
                SessionStatus.WAITING,
                org.springframework.data.domain.PageRequest.of(0, 1000)
        );

        for (ChatSession session : waitingSessions.getContent()) {
            // 대기열에서 먼저 제거 (종료 전!)
            consultantService.removeFromWaitingQueue(session.getId());

            // 세션 종료
            session.close();

            log.info("대기 세션 종료 및 대기열 제거: sessionId={}, userId={}", session.getId(), userId);
        }

        sessionRepository.saveAll(waitingSessions.getContent());
    }

    /**
     * 세션 메시지 조회
     */
    public List<ChatResponse> getSessionMessages(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
        return messages.stream()
                .map(ChatResponse::from)
                .toList();
    }

    // ===== Private Helper Methods =====

    private void validateMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new GlobalException(ChatbotErrorCode.EMPTY_MESSAGE);
        }
        if (content.length() > 2000) {
            throw new GlobalException(ChatbotErrorCode.MESSAGE_TOO_LONG);
        }
    }

    private boolean checkRateLimit(Long userId) {
        if (!globalRateLimiter.tryAcquire()) {
            log.warn("글로벌 Rate Limit 초과");
            return false;
        }

        RateLimiter userLimiter = userRateLimiters.computeIfAbsent(
                userId,
                k -> RateLimiter.create(10.0 / 60.0)
        );

        if (!userLimiter.tryAcquire()) {
            log.warn("사용자 Rate Limit 초과: userId={}", userId);
            return false;
        }

        return true;
    }

    private void validateSessionAccess(ChatSession session, Long userId) {
        if (!session.isOwnedBy(userId)) {
            throw new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    /**
     * 메시지 → Gemini 메시지 변환
     */
    private List<GeminiFunction.GeminiMessage> convertToGeminiMessages(List<ChatMessage> messages) {
        List<GeminiFunction.GeminiMessage> geminiMessages = new ArrayList<>();

        // 최근 메시지부터 역순이므로 다시 정렬
        messages = new ArrayList<>(messages);
        java.util.Collections.reverse(messages);

        for (ChatMessage message : messages) {
            // CONSULTANT, SYSTEM 메시지는 제외
            if (message.getType() == MessageType.CONSULTANT ||
                    message.getType() == MessageType.SYSTEM) {
                continue;
            }

            String role = switch (message.getType()) {
                case USER -> "user";
                case AI -> "model";
                default -> null;
            };

            if (role != null) {
                geminiMessages.add(GeminiFunction.GeminiMessage.builder()
                        .role(role)
                        .content(message.getContent())
                        .build());
            }
        }

        return geminiMessages;
    }

    /**
     * 상담원 연결 키워드 감지
     */
    private boolean isConsultantRequestKeyword(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String lowerText = content.toLowerCase();

        String[] consultantKeywords = {
                "상담원", "상담원 연결", "상담사", "직원", "담당자",
                "사람과 통화", "사람과 얘기", "실제 사람"
        };

        for (String keyword : consultantKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}