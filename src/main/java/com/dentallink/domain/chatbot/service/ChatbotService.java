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
 * 챗봇 메인 서비스
 * - 채팅 세션 관리
 * - AI 응답 생성
 * - Rate Limiting
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

    // 글로벌 Rate Limiter (Gemini API 전체 제한)
    private final RateLimiter globalRateLimiter = RateLimiter.create(15.0 / 60.0); // 분당 15회

    // 사용자별 Rate Limiter
    private final ConcurrentHashMap<Long, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    /**
     * 메시지 처리 및 AI 응답 생성 (WebSocket용)
     * - 상담원 모드 체크 포함
     */
    @Transactional
    public ChatResponse processMessage(ChatRequest request, Long userId) {

        // 1. 입력 검증
        validateMessage(request.content());

        // 2. 세션 가져오기 또는 생성
        ChatSession session = getOrCreateSession(request.sessionId(), userId);

        // 3. 상담원 모드 체크
        if (session.isConsultantMode()) {
            return handleConsultantMessage(session, request, userId);
        }

        // 4. 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

        // 5. Rate Limit 체크
        if (!checkRateLimit(userId)) {
            // Rate Limit 초과 → 상담원 전환
            return handleRateLimitExceeded(session, userId);
        }

        try {
            // 6. AI 응답 생성
            return generateAIResponse(session, userId);

        } catch (GlobalException e) {
            if (e.getErrorCode() == ChatbotErrorCode.RATE_LIMIT_EXCEEDED) {
                return handleRateLimitExceeded(session, userId);
            }
            throw e;
        }
    }

    /**
     * 상담원 모드 메시지 처리
     */
    private ChatResponse handleConsultantMessage(ChatSession session, ChatRequest request, Long userId) {
        log.info("상담원 모드 메시지 처리: sessionId={}, userId={}", session.getId(), userId);

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        messageRepository.save(userMessage);

        // 상담원에게 메시지 전달
        if (session.getConsultant() != null) {
            messagingTemplate.convertAndSendToUser(
                    session.getConsultant().getId().toString(),
                    "/queue/reply",
                    ChatResponse.from(userMessage)
            );
        }

        return ChatResponse.from(userMessage);
    }

    /**
     * AI 응답 생성
     */
    private ChatResponse generateAIResponse(ChatSession session, Long userId) {

        // 1. 대화 히스토리 가져오기 (최근 10개)
        List<ChatMessage> recentMessages = messageRepository.findRecentMessages(session.getId(), 10);

        // 2. Gemini 메시지 형식으로 변환
        List<GeminiFunction.GeminiMessage> geminiMessages = convertToGeminiMessages(recentMessages);

        // 3. Function 선언 가져오기
        List<GeminiFunction.FunctionDeclaration> functions = functionCallHandler.getFunctionDeclarations();

        // 4. 시스템 프롬프트 추가
        geminiMessages.add(0, createSystemPrompt());

        // 5. Gemini API 호출
        GeminiApiService.GeminiApiResponse geminiResponse =
                geminiApiService.generateContent(geminiMessages, functions);

        // 6. Function Call 처리
        if (geminiResponse.hasFunctionCalls()) {
            return handleFunctionCalls(session, geminiResponse, geminiMessages, functions, userId);
        }

        // 7. 일반 응답 저장 및 반환
        ChatMessage aiMessage = ChatMessage.createAIMessage(session, geminiResponse.text());
        messageRepository.save(aiMessage);

        return ChatResponse.from(aiMessage);
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

        // 각 Function Call 실행
        for (GeminiFunction.FunctionCall functionCall : geminiResponse.functionCalls()) {
            Object result = functionCallHandler.executeFunction(functionCall, userId);

            functionResponses.add(GeminiFunction.FunctionResponse.builder()
                    .name(functionCall.name())
                    .response(result)
                    .build());
        }

        // Function Call과 결과를 대화에 추가
        conversationHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("model")
                .content("")
                .functionCalls(geminiResponse.functionCalls())
                .build());

        conversationHistory.add(GeminiFunction.GeminiMessage.builder()
                .role("function")
                .content("")
                .functionResponses(functionResponses)
                .build());

        // 다시 Gemini 호출하여 최종 사용자 응답 생성
        GeminiApiService.GeminiApiResponse finalResponse =
                geminiApiService.generateContent(conversationHistory, functions);

        // AI 메시지 저장 (Function 정보 포함)
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
     * Rate Limit 초과 시 상담원 전환
     */
    private ChatResponse handleRateLimitExceeded(ChatSession session, Long userId) {
        log.info("Rate Limit 초과 - 상담원 전환: userId={}", userId);

        // 시스템 메시지 저장
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "현재 문의가 많아 상담원을 연결해드리겠습니다."
        );
        messageRepository.save(systemMessage);

        // 상담원 연결 시도
        var matchResult = consultantService.transferToConsultant(session.getId(), userId);

        return ChatResponse.createTransferResponse(session.getId(), matchResult.waitingPosition());
    }

    /**
     * 세션 가져오기 또는 생성
     */
    private ChatSession getOrCreateSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

            validateSessionAccess(session, userId);

            if (session.getStatus() == SessionStatus.CLOSED) {
                throw new GlobalException(ChatbotErrorCode.SESSION_ALREADY_CLOSED);
            }

            return session;
        }

        // 새 세션 생성
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS));

        ChatSession newSession = ChatSession.startAISession(user);
        return sessionRepository.save(newSession);
    }

    /**
     * 세션 종료
     */
    @Transactional
    public void closeSession(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GlobalException(ChatbotErrorCode.SESSION_NOT_FOUND));

        validateSessionAccess(session, userId);

        session.close();

        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                session,
                "상담이 종료되었습니다. 이용해 주셔서 감사합니다."
        );
        messageRepository.save(systemMessage);
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
     * 세션의 메시지 목록 조회
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

    /**
     * 메시지 검증
     */
    private void validateMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new GlobalException(ChatbotErrorCode.EMPTY_MESSAGE);
        }
        if (content.length() > 2000) {
            throw new GlobalException(ChatbotErrorCode.MESSAGE_TOO_LONG);
        }
    }

    /**
     * Rate Limit 체크
     */
    private boolean checkRateLimit(Long userId) {
        // 1. 글로벌 Rate Limit
        if (!globalRateLimiter.tryAcquire()) {
            log.warn("글로벌 Rate Limit 초과");
            return false;
        }

        // 2. 사용자별 Rate Limit
        RateLimiter userLimiter = userRateLimiters.computeIfAbsent(
                userId,
                k -> RateLimiter.create(10.0 / 60.0)  // 사용자당 분당 10회
        );

        if (!userLimiter.tryAcquire()) {
            log.warn("사용자 Rate Limit 초과: userId={}", userId);
            return false;
        }

        return true;
    }

    /**
     * 세션 접근 권한 검증
     */
    private void validateSessionAccess(ChatSession session, Long userId) {
        if (!session.isOwnedBy(userId)) {
            throw new GlobalException(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    /**
     * Gemini 메시지 형식으로 변환
     */
    private List<GeminiFunction.GeminiMessage> convertToGeminiMessages(List<ChatMessage> messages) {
        List<GeminiFunction.GeminiMessage> geminiMessages = new ArrayList<>();

        // 최근 메시지부터 역순이므로 다시 정렬
        messages = new ArrayList<>(messages);
        java.util.Collections.reverse(messages);

        for (ChatMessage message : messages) {
            String role = switch (message.getType()) {
                case USER -> "user";
                case AI, SYSTEM -> "model";
                default -> "model";
            };

            geminiMessages.add(GeminiFunction.GeminiMessage.builder()
                    .role(role)
                    .content(message.getContent())
                    .build());
        }

        return geminiMessages;
    }

    /**
     * 시스템 프롬프트 생성
     */
    private GeminiFunction.GeminiMessage createSystemPrompt() {
        String systemPrompt = """
                당신은 DentalLink 치과 예약 시스템의 친절한 AI 상담사입니다.

                주요 역할:
                1. 사용자의 예약 관련 질문에 답변
                2. 예약 가능 시간 조회 및 안내
                3. 예약 생성, 조회, 취소 지원
                4. 병원 정보 제공 (이름, 위치, 의사 검색)

                응답 가이드:
                - 친절하고 전문적인 톤 사용
                - 간결하고 명확한 답변 제공
                - 예약 생성 시 포인트 차감 사실 안내
                - 복잡한 문의는 상담원 연결 제안
                - 항상 한국어로 응답

                제공 가능한 기능:
                - get_available_times: 예약 가능 시간 조회
                - create_reservation: 예약 생성
                - get_my_reservations: 내 예약 조회
                - cancel_reservation: 예약 취소
                - search_hospitals: 병원을 이름으로 검색
                - search_hospitals_by_location: 병원을 위치/지역/주소로 검색 (예: "강남 지역 병원", "서초동 병원")
                - search_hospitals_by_doctor: 특정 의사가 근무하는 병원을 검색 (예: "김철수 의사 병원", "이영희 선생님 있는 병원")
                """;

        return GeminiFunction.GeminiMessage.builder()
                .role("user")
                .content(systemPrompt)
                .build();
    }
}