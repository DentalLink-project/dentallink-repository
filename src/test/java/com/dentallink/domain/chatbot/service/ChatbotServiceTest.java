package com.dentallink.domain.chatbot.service;

import com.dentallink.common.config.GeminiConfig;
import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.chatbot.dto.ChatRequest;
import com.dentallink.domain.chatbot.dto.ChatResponse;
import com.dentallink.domain.chatbot.dto.GeminiFunction;
import com.dentallink.domain.chatbot.entity.ChatMessage;
import com.dentallink.domain.chatbot.entity.ChatSession;
import com.dentallink.domain.chatbot.enums.ChatMode;
import com.dentallink.domain.chatbot.enums.MessageType;
import com.dentallink.domain.chatbot.enums.SessionStatus;
import com.dentallink.domain.chatbot.exception.ChatbotErrorCode;
import com.dentallink.domain.chatbot.repository.ChatMessageRepository;
import com.dentallink.domain.chatbot.repository.ChatSessionRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotService - 단위 테스트")
class ChatbotServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiApiService geminiApiService;

    @Mock
    private FunctionCallHandler functionCallHandler;

    @Mock
    private ConsultantService consultantService;

    @Mock
    private GeminiConfig geminiConfig;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatbotService chatbotService;

    private User user;
    private User consultant;
    private ChatSession chatSession;
    private ChatSession consultantSession;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        user = User.of(
                "test@naver.com",
                "password123",
                "테스트유저",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        // 상담원 사용자 생성
        consultant = User.of(
                "consultant@naver.com",
                "password123",
                "상담원",
                UserRole.ROLE_ADMIN
        );
        ReflectionTestUtils.setField(consultant, "id", 2L);

        // AI 모드 세션 생성
        chatSession = ChatSession.startAISession(user);
        ReflectionTestUtils.setField(chatSession, "id", 1L);

        // 상담원 모드 세션 생성
        consultantSession = ChatSession.startAISession(user);
        consultantSession.transferToConsultant(consultant);
        ReflectionTestUtils.setField(consultantSession, "id", 2L);
    }

    // ===== 세션 생성 테스트 =====

    @Test
    @DisplayName("새 메시지 요청 시 새 세션이 생성된다")
    void shouldCreateNewSessionWhenSessionIdIsNull() {
        // Given
        ChatRequest request = new ChatRequest(null, "안녕하세요");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(sessionRepository.save(any(ChatSession.class))).willReturn(chatSession);
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(messageRepository.findRecentMessages(1L, 10)).willReturn(new ArrayList<>());
        given(functionCallHandler.getFunctionDeclarations()).willReturn(new ArrayList<>());
        given(geminiApiService.generateContent(any(), any()))
                .willReturn(new GeminiApiService.GeminiApiResponse("응답입니다", new ArrayList<>()));

        // When
        ChatResponse response = chatbotService.processMessage(request, 1L);

        // Then
        assertThat(response).isNotNull();
        then(userRepository).should().findById(1L);
        then(sessionRepository).should().save(any(ChatSession.class));
    }

    @Test
    @DisplayName("존재하는 세션에서 메시지를 처리할 수 있다")
    void shouldProcessMessageInExistingSession() {
        // Given
        ChatRequest request = new ChatRequest(1L, "시간대 추천해주세요");
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(messageRepository.findRecentMessages(1L, 10)).willReturn(new ArrayList<>());
        given(functionCallHandler.getFunctionDeclarations()).willReturn(new ArrayList<>());
        given(geminiApiService.generateContent(any(), any()))
                .willReturn(new GeminiApiService.GeminiApiResponse("추천하는 시간입니다", new ArrayList<>()));

        // When
        ChatResponse response = chatbotService.processMessage(request, 1L);

        // Then
        assertThat(response).isNotNull();
        then(sessionRepository).should().findById(1L);
        then(messageRepository).should(times(2)).save(any(ChatMessage.class)); // 사용자 메시지 + AI 응답
    }

    // ===== 상담원 연결 키워드 테스트 =====

    @Test
    @DisplayName("상담원 연결 키워드 감지 시 상담원으로 전환된다")
    void shouldTransferToConsultantWhenKeywordDetected() {
        // Given
        ChatRequest request = new ChatRequest(1L, "상담원 연결해주세요");
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(consultantService.transferToConsultant(1L, 1L))
                .willReturn(new ConsultantService.ConsultantMatchResult(true, 2L, 0L));

        // When
        ChatResponse response = chatbotService.processMessage(request, 1L);

        // Then
        assertThat(response).isNotNull();
        then(consultantService).should().transferToConsultant(1L, 1L);
        then(messageRepository).should(times(2)).save(any(ChatMessage.class)); // 사용자 메시지 + 시스템 메시지
    }

    @Test
    @DisplayName("여러 상담원 관련 키워드를 감지할 수 있다")
    void shouldDetectMultipleConsultantKeywords() {
        // Given
        String[] keywords = {
                "상담원 연결",
                "상담사 연결해주세요",
                "직원과 통화",
                "사람하고 얘기하고 싶어요",
                "talk to agent"
        };

        for (String keyword : keywords) {
            ChatRequest request = new ChatRequest(1L, keyword);
            given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));
            given(messageRepository.save(any(ChatMessage.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(consultantService.transferToConsultant(1L, 1L))
                    .willReturn(new ConsultantService.ConsultantMatchResult(true, 2L, 0L));

            // When
            ChatResponse response = chatbotService.processMessage(request, 1L);

            // Then
            assertThat(response).isNotNull();
        }
    }

    // ===== 상담원 모드 메시지 처리 =====

    @Test
    @DisplayName("상담원 모드에서 메시지를 처리한다")
    void shouldHandleConsultantModeMessage() {
        // Given
        ChatRequest request = new ChatRequest(2L, "네, 알겠습니다");
        given(sessionRepository.findById(2L)).willReturn(Optional.of(consultantSession));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        ChatResponse response = chatbotService.processMessage(request, 1L);

        // Then
        assertThat(response).isNotNull();
        then(messageRepository).should().save(any(ChatMessage.class));
        then(messagingTemplate).should().convertAndSendToUser(eq("2"), eq("/queue/messages"), any(ChatResponse.class));
    }

    // ===== 세션 종료 테스트 =====

    @Test
    @DisplayName("세션을 종료할 수 있다")
    void shouldCloseSession() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        chatbotService.closeSession(1L, 1L);

        // Then
        assertThat(chatSession.getStatus()).isEqualTo(SessionStatus.CLOSED);
        then(messageRepository).should().save(any(ChatMessage.class)); // 시스템 메시지 저장
    }

    @Test
    @DisplayName("자신의 세션이 아니면 종료할 수 없다")
    void shouldNotCloseSessionOfOtherUser() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));

        // When & Then
        assertThatThrownBy(() -> chatbotService.closeSession(1L, 999L))
                .isInstanceOf(GlobalException.class);
    }

    // ===== 세션 조회 테스트 =====

    @Test
    @DisplayName("세션 상세 정보를 조회할 수 있다")
    void shouldGetSessionDetails() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));

        // When
        var response = chatbotService.getSession(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.sessionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 세션을 조회하면 예외가 발생한다")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Given
        given(sessionRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatbotService.getSession(999L, 1L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("닫힌 세션에 메시지를 보낼 수 없다")
    void shouldNotProcessMessageInClosedSession() {
        // Given
        chatSession.close();
        ChatRequest request = new ChatRequest(1L, "안녕");
        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));

        // When & Then
        assertThatThrownBy(() -> chatbotService.processMessage(request, 1L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.SESSION_ALREADY_CLOSED);
    }

    // ===== 메시지 검증 테스트 =====

    @Test
    @DisplayName("빈 메시지는 처리할 수 없다")
    void shouldRejectEmptyMessage() {
        // Given
        ChatRequest request = new ChatRequest(null, "");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> chatbotService.processMessage(request, 1L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.EMPTY_MESSAGE);
    }

    @Test
    @DisplayName("2000자를 초과하는 메시지는 처리할 수 없다")
    void shouldRejectTooLongMessage() {
        // Given
        String longMessage = "가".repeat(2001);
        ChatRequest request = new ChatRequest(null, longMessage);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> chatbotService.processMessage(request, 1L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.MESSAGE_TOO_LONG);
    }

    @Test
    @DisplayName("공백만으로 이루어진 메시지는 처리할 수 없다")
    void shouldRejectWhitespaceOnlyMessage() {
        // Given
        ChatRequest request = new ChatRequest(null, "   ");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> chatbotService.processMessage(request, 1L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.EMPTY_MESSAGE);
    }

    // ===== 세션 목록 조회 테스트 =====

    @Test
    @DisplayName("사용자의 세션 목록을 조회할 수 있다")
    void shouldGetUserSessions() {
        // Given
        var pageable = PageRequest.of(0, 10);
        given(sessionRepository.findByUserIdOrderByStartedAtDesc(1L, pageable))
                .willReturn(new PageImpl<>(List.of(chatSession), pageable, 1));

        // When
        var result = chatbotService.getMySessions(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    // ===== 세션 메시지 조회 테스트 =====

    @Test
    @DisplayName("세션의 모든 메시지를 조회할 수 있다")
    void shouldGetSessionMessages() {
        // Given
        ChatMessage userMsg = ChatMessage.createUserMessage(chatSession, "안녕");
        ChatMessage aiMsg = ChatMessage.createAIMessage(chatSession, "반갑습니다");
        ReflectionTestUtils.setField(userMsg, "id", 1L);
        ReflectionTestUtils.setField(aiMsg, "id", 2L);

        given(sessionRepository.findById(1L)).willReturn(Optional.of(chatSession));
        given(messageRepository.findBySessionIdOrderBySentAtAsc(1L))
                .willReturn(List.of(userMsg, aiMsg));

        // When
        var messages = chatbotService.getSessionMessages(1L, 1L);

        // Then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).type()).isEqualTo(MessageType.USER);
        assertThat(messages.get(1).type()).isEqualTo(MessageType.AI);
    }

    // ===== 모든 세션 종료 테스트 =====

    @Test
    @DisplayName("사용자의 모든 활성 세션을 종료할 수 있다")
    void shouldCloseAllActiveSessions() {
        // Given
        ChatSession session2 = ChatSession.startAISession(user);
        ReflectionTestUtils.setField(session2, "id", 3L);

        var pageable = PageRequest.of(0, 1000);
        given(sessionRepository.findByUserIdAndStatus(1L, SessionStatus.ACTIVE, pageable))
                .willReturn(new PageImpl<>(List.of(chatSession, session2), pageable, 2));

        // When
        chatbotService.closeAllSessionsForUser(1L);

        // Then
        assertThat(chatSession.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session2.getStatus()).isEqualTo(SessionStatus.CLOSED);
        then(sessionRepository).should().saveAll(anyList());
    }
}