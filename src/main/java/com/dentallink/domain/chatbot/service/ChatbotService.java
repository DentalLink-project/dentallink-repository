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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 챗봇 메인 서비스 - DB 중심 개선 버전
 * - 대화 히스토리 최소화
 * - Function Calling 우선
 * - 단일 턴 대화 중심
 */
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

    /**
     * ✅ 개선: 대화 히스토리 최소화, DB 기반 답변 우선
     */
    @Transactional
    public ChatResponse processMessage(ChatRequest request, Long userId) {

        validateMessage(request.content());
        ChatSession session = getOrCreateSession(request.sessionId(), userId);

        if (session.isConsultantMode()) {
            return handleConsultantMessage(session, request, userId);
        }

        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

        if (isConsultantRequestKeyword(request.content())) {
            log.info("상담원 연결 요청 감지: userId={}", userId);

            ChatMessage systemMessage = ChatMessage.createSystemMessage(
                    session,
                    "상담원 연결을 요청하셨습니다. 상담원을 연결해드리겠습니다."
            );
            messageRepository.save(systemMessage);

            var matchResult = consultantService.transferToConsultant(session.getId(), userId);
            return ChatResponse.createTransferResponse(session.getId(), matchResult.waitingPosition());
        }

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

    private ChatResponse generateAIResponseOptimized(ChatSession session, Long userId, String currentUserInput) {

        // 1. 대화 히스토리 최소화 (최근 3개만 - 컨텍스트 파악용)
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(session.getId(), 3);
        List<GeminiFunction.GeminiMessage> geminiMessages = convertToGeminiMessages(recentMessages);

        // 2. Function 선언 가져오기
        List<GeminiFunction.FunctionDeclaration> functions = functionCallHandler.getFunctionDeclarations();

        geminiMessages.add(0, createDBFocusedSystemPrompt());

        if (!geminiMessages.isEmpty()) {
            // 마지막 메시지가 사용자 메시지가 아니면 추가
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


    private GeminiFunction.GeminiMessage createDBFocusedSystemPrompt() {
        String systemPrompt = """
                당신은 DentalLink 치과 예약 시스템의 AI 상담사입니다.
                
                🎯 핵심 원칙: 데이터베이스 우선 답변
                - 사용자 질문에 대해 항상 Function Call을 먼저 고려하세요
                - 추측하지 말고, DB에서 정확한 정보를 조회하세요
                - 대화 맥락보다 현재 질문에 집중하세요
                
                📌 필수 행동 규칙:
                1. 병원 관련 질문 → 즉시 search_hospitals* 함수 호출
                2. 예약 관련 질문 → 즉시 get_my_reservations 또는 get_available_times 호출
                3. 불확실한 정보는 Function Call로 확인 후 답변
                4. 대화 맥락 기억보다 실시간 DB 조회 우선
                
                ❌ 하지 말아야 할 것:
                - "이전에 말씀하신 것처럼..." 같은 대화 맥락 언급
                - 추측성 답변 ("아마도...", "~일 것 같습니다")
                - Function Call 없이 병원명이나 예약 정보 언급
                
                ✅ 올바른 응답 예시:
                Q: "강남에 있는 치과 알려줘"
                A: [search_hospitals_by_location 호출] → DB 결과 기반 답변
                
                Q: "내 예약 보여줘"
                A: [get_my_reservations 호출] → 실제 예약 내역 제공
                
                Q: "그 병원 예약 가능한 시간은?"
                A: [search_hospitals + get_available_times 호출] → 정확한 시간대 제공
                
                🔧 제공 가능한 기능:
                - search_hospitals: 병원 이름 검색 (정확한 이름 필요)
                - search_hospitals_by_location: 지역/주소로 병원 검색 (예: "강남", "서초동")
                - search_hospitals_by_doctor: 의사 이름으로 병원 검색
                - get_available_times: 특정 병원의 예약 가능 시간 조회
                - create_reservation: 예약 생성 (1000P 차감)
                - get_my_reservations: 내 예약 목록 조회
                - cancel_reservation: 예약 취소
                
                💬 응답 스타일:
                - 간결하고 정확하게 (DB 결과 기반)
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
     * ✅ 개선: Function Call 처리 시 히스토리 최소화
     */
    private ChatResponse handleFunctionCalls(
            ChatSession session,
            GeminiApiService.GeminiApiResponse geminiResponse,
            List<GeminiFunction.GeminiMessage> conversationHistory,
            List<GeminiFunction.FunctionDeclaration> functions,
            Long userId) {

        List<GeminiFunction.FunctionResponse> functionResponses = new ArrayList<>();

        // 각 Function Call 실행
        for (GeminiFunction.FunctionCall functionCall : geminiResponse.functionCalls()) {
            Object result = functionCallHandler.executeFunction(functionCall, userId);

            functionResponses.add(GeminiFunction.FunctionResponse.builder()
                    .name(functionCall.name())
                    .response(result)
                    .build());
        }

        // ✅ 개선: Function 결과만으로 새로운 대화 구성
        List<GeminiFunction.GeminiMessage> simplifiedHistory = new ArrayList<>();

        // 시스템 프롬프트
        simplifiedHistory.add(createDBFocusedSystemPrompt());

        // 최근 사용자 메시지만 (마지막 1개)
        conversationHistory.stream()
                .filter(msg -> "user".equals(msg.role()))
                .reduce((first, second) -> second)  // 마지막 것만
                .ifPresent(simplifiedHistory::add);

        // Function Call
        simplifiedHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("model")
                .content("")
                .functionCalls(geminiResponse.functionCalls())
                .build());

        // Function Response
        simplifiedHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("function")
                .content("")
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

    // ===== 기존 메서드들 (변경 없음) =====

    private ChatResponse handleConsultantMessage(ChatSession session, ChatRequest request, Long userId) {
        log.info("상담원 모드 메시지 처리: sessionId={}, userId={}", session.getId(), userId);

        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

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

    private ChatSession getOrCreateSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

            validateSessionAccess(session, userId);
            return session;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        ChatSession newSession = ChatSession.startAISession(user);
        return sessionRepository.save(newSession);
    }

    @Transactional
    public void closeSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        session.close();

        log.info("세션 종료: sessionId={}, userId={}", sessionId, userId);
    }

    public SessionResponse getSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        return SessionResponse.from(session);
    }

    public Page<SessionResponse> getMySessions(Long userId, Pageable pageable) {
        Page<ChatSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
        return sessions.map(SessionResponse::from);
    }

    @Transactional
    public void closeAllSessionsForUser(Long userId) {
        log.info("사용자 로그아웃 - 활성 세션 정리: userId={}", userId);

        Page<ChatSession> activeSessions = sessionRepository.findByUserIdAndStatus(
                userId,
                SessionStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, 1000)
        );

        for (ChatSession session : activeSessions.getContent()) {
            session.close();
            log.info("세션 종료: sessionId={}, userId={}", session.getId(), userId);
        }

        sessionRepository.saveAll(activeSessions.getContent());
    }

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
     * ✅ 개선: 메시지 변환 시 타입 명시적 처리
     */
    private List<GeminiFunction.GeminiMessage> convertToGeminiMessages(List<ChatMessage> messages) {
        List<GeminiFunction.GeminiMessage> geminiMessages = new ArrayList<>();

        // 최근 메시지부터 역순이므로 다시 정렬
        messages = new ArrayList<>(messages);
        java.util.Collections.reverse(messages);

        for (ChatMessage message : messages) {
            // CONSULTANT, SYSTEM 메시지는 제외 (DB 조회 결과에 집중)
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

    private boolean isConsultantRequestKeyword(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String lowerText = content.toLowerCase();

        String[] consultantKeywords = {
                "상담원", "상담원 연결", "상담사", "직원", "담당자",
                "사람과 통화", "사람과 얘기"
        };

        for (String keyword : consultantKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}