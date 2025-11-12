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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

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
@DisplayName("ConsultantService - 상담원 서비스 단위 테스트")
class ConsultantServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOps;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private ConsultantService consultantService;

    private User user;
    private User consultant;
    private User consultant2;
    private ChatSession session;
    private ChatSession session2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        user = User.of(
                "user@naver.com",
                "password123",
                "테스트사용자",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        // 상담원 1 생성
        consultant = User.of(
                "consultant1@naver.com",
                "password123",
                "상담원1",
                UserRole.ROLE_ADMIN
        );
        ReflectionTestUtils.setField(consultant, "id", 2L);

        // 상담원 2 생성
        consultant2 = User.of(
                "consultant2@naver.com",
                "password123",
                "상담원2",
                UserRole.ROLE_ADMIN
        );
        ReflectionTestUtils.setField(consultant2, "id", 3L);

        // 세션 생성
        session = ChatSession.startAISession(user);
        ReflectionTestUtils.setField(session, "id", 1L);

        session2 = ChatSession.startAISession(user);
        ReflectionTestUtils.setField(session2, "id", 2L);

        // Redis 모킹 설정
        given(redisTemplate.opsForList()).willReturn(listOps);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ===== 상담원 전환 테스트 =====

    @Test
    @DisplayName("가용 상담원이 있으면 즉시 상담원에게 전환된다")
    void shouldTransferToAvailableConsultantImmediately() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(userRepository.findAll()).willReturn(List.of(consultant));
        given(sessionRepository.findByConsultantIdAndStatus(2L, SessionStatus.ACTIVE))
                .willReturn(new ArrayList<>()); // 상담원 활성 세션 없음
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = consultantService.transferToConsultant(1L, 1L);

        // Then
        assertThat(result.connected()).isTrue();
        assertThat(result.consultantId()).isEqualTo(2L);
        assertThat(result.waitingPosition()).isEqualTo(0L);
        assertThat(session.isConsultantMode()).isTrue();
        then(messageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("가용 상담원이 없으면 Redis 대기열에 추가된다")
    void shouldAddToWaitingQueueWhenNoConsultantAvailable() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(userRepository.findAll()).willReturn(List.of(consultant));
        // 상담원 활성 세션 3개 시뮬레이션 (페이크 객체로 리스트 생성)
        ChatSession activeSession1 = ChatSession.startAISession(user);
        ChatSession activeSession2 = ChatSession.startAISession(user);
        ChatSession activeSession3 = ChatSession.startAISession(user);
        given(sessionRepository.findByConsultantIdAndStatus(2L, SessionStatus.ACTIVE))
                .willReturn(List.of(activeSession1, activeSession2, activeSession3));
        given(listOps.rightPush("chatbot:waiting:queue", "1")).willReturn(1L);
        given(listOps.size("chatbot:waiting:queue")).willReturn(1L);

        // When
        var result = consultantService.transferToConsultant(1L, 1L);

        // Then
        assertThat(result.connected()).isFalse();
        assertThat(result.waitingPosition()).isEqualTo(1L);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.WAITING);
        then(listOps).should().rightPush("chatbot:waiting:queue", "1");
        then(valueOps).should().set(contains("chatbot:session:position:"), any());
    }

    @Test
    @DisplayName("이미 상담원 모드인 세션은 재전환되지 않는다")
    void shouldNotTransferAlreadyConsultantMode() {
        // Given
        session.transferToConsultant(consultant);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        // When
        var result = consultantService.transferToConsultant(1L, 1L);

        // Then
        assertThat(result.connected()).isTrue();
        assertThat(result.consultantId()).isEqualTo(2L);
        assertThat(result.waitingPosition()).isEqualTo(0L);
    }

    // ===== 상담원 메시지 전송 테스트 =====

    @Test
    @DisplayName("상담원이 메시지를 전송할 수 있다")
    void shouldSendConsultantMessage() {
        // Given
        session.transferToConsultant(consultant);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        consultantService.sendConsultantMessage(1L, 2L, "네, 알겠습니다");

        // Then
        then(messageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("상담원이 아닌 사용자는 메시지를 전송할 수 없다")
    void shouldNotSendMessageWrongConsultant() {
        // Given
        session.transferToConsultant(consultant);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> consultantService.sendConsultantMessage(1L, 999L, "메시지"))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
    }

    @Test
    @DisplayName("AI 모드 세션에는 상담원이 메시지를 보낼 수 없다")
    void shouldNotSendMessageToAISession() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> consultantService.sendConsultantMessage(1L, 2L, "메시지"))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.NOT_CONSULTANT_SESSION);
    }

    // ===== 상담원 세션 조회 테스트 =====

    @Test
    @DisplayName("상담원이 담당 중인 세션 목록을 조회할 수 있다")
    void shouldGetConsultantActiveSessions() {
        // Given
        ChatSession activeSession = ChatSession.startAISession(user);
        activeSession.transferToConsultant(consultant);
        given(sessionRepository.findByConsultantIdAndStatus(2L, SessionStatus.ACTIVE))
                .willReturn(List.of(activeSession));

        // When
        var sessions = consultantService.getConsultantSessions(2L);

        // Then
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getConsultant().getId()).isEqualTo(2L);
    }

    // ===== 세션 종료 테스트 =====

    @Test
    @DisplayName("상담원이 세션을 종료할 수 있다")
    void shouldCloseConsultantSession() {
        // Given
        session.transferToConsultant(consultant);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        consultantService.closeConsultantSession(1L, 2L);

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        then(messageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("해당 상담원이 아니면 세션을 종료할 수 없다")
    void shouldNotCloseSessionWrongConsultant() {
        // Given
        session.transferToConsultant(consultant);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        // When & Then
        assertThatThrownBy(() -> consultantService.closeConsultantSession(1L, 999L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.UNAUTHORIZED_ACCESS);
    }

    // ===== 특정 세션 선택 테스트 =====

    @Test
    @DisplayName("상담원이 대기 중인 세션을 선택하여 수락할 수 있다")
    void shouldPickSpecificWaitingSession() {
        // Given
        session.moveToWaitingPosition(1L);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(userRepository.findById(2L)).willReturn(Optional.of(consultant));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(listOps.size("chatbot:waiting:queue")).willReturn(1L);

        // When
        var result = consultantService.pickSpecificSession(1L, 2L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConsultant().getId()).isEqualTo(2L);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        then(listOps).should().remove("chatbot:waiting:queue", 1, "1");
    }

    @Test
    @DisplayName("대기 상태가 아닌 세션은 선택할 수 없다")
    void shouldNotPickNonWaitingSession() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session)); // ACTIVE 상태
        given(listOps.size("chatbot:waiting:queue")).willReturn(0L);

        // When
        var result = consultantService.pickSpecificSession(1L, 2L);

        // Then
        assertThat(result).isEmpty();
    }

    // ===== 다음 대기 세션 조회 테스트 =====

    @Test
    @DisplayName("상담원이 대기 중인 다음 세션을 가져올 수 있다")
    void shouldPickNextWaitingSession() {
        // Given
        session.moveToWaitingPosition(1L);
        given(listOps.leftPop("chatbot:waiting:queue"))
                .willReturn("1");
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(userRepository.findById(2L)).willReturn(Optional.of(consultant));
        given(messageRepository.save(any(ChatMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = consultantService.pickNextWaitingSession(2L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConsultant().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("대기열이 비면 빈 Optional을 반환한다")
    void shouldReturnEmptyWhenQueueIsEmpty() {
        // Given
        given(listOps.leftPop("chatbot:waiting:queue")).willReturn(null);

        // When
        var result = consultantService.pickNextWaitingSession(2L);

        // Then
        assertThat(result).isEmpty();
    }

    // ===== 대기열 상태 조회 테스트 =====

    @Test
    @DisplayName("대기열의 상태를 조회할 수 있다")
    void shouldGetQueueStatus() {
        // Given
        given(listOps.size("chatbot:waiting:queue")).willReturn(5L);
        given(userRepository.findAll()).willReturn(List.of(consultant, consultant2));

        // When
        var status = consultantService.getQueueStatus();

        // Then
        assertThat(status.waitingCount()).isEqualTo(5);
        assertThat(status.activeConsultants()).isEqualTo(2);
    }

    @Test
    @DisplayName("대기열이 비어있으면 0을 반환한다")
    void shouldReturnZeroQueueCountWhenEmpty() {
        // Given
        given(listOps.size("chatbot:waiting:queue")).willReturn(0L);
        given(userRepository.findAll()).willReturn(List.of(consultant));

        // When
        var status = consultantService.getQueueStatus();

        // Then
        assertThat(status.waitingCount()).isEqualTo(0);
    }

    // ===== 대기 세션 목록 조회 테스트 =====

    @Test
    @DisplayName("대기 중인 세션 목록을 조회할 수 있다")
    void shouldGetWaitingSessions() {
        // Given
        session.moveToWaitingPosition(1L);
        session2.moveToWaitingPosition(2L);
        given(listOps.range("chatbot:waiting:queue", 0, -1))
                .willReturn(List.of("1", "2"));
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(sessionRepository.findById(2L)).willReturn(Optional.of(session2));

        // When
        var waitingSessions = consultantService.getWaitingSessions();

        // Then
        assertThat(waitingSessions).hasSize(2);
        assertThat(waitingSessions.get(0).sessionId()).isEqualTo(1L);
        assertThat(waitingSessions.get(0).waitingPosition()).isEqualTo(1L);
        assertThat(waitingSessions.get(1).sessionId()).isEqualTo(2L);
        assertThat(waitingSessions.get(1).waitingPosition()).isEqualTo(2L);
    }

    @Test
    @DisplayName("대기 상태가 아닌 세션은 목록에서 제외된다")
    void shouldExcludeNonWaitingSessionsFromList() {
        // Given
        session.moveToWaitingPosition(1L);
        session2.transferToConsultant(consultant); // ACTIVE 상태로 변경
        given(listOps.range("chatbot:waiting:queue", 0, -1))
                .willReturn(List.of("1", "2"));
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(sessionRepository.findById(2L)).willReturn(Optional.of(session2));

        // When
        var waitingSessions = consultantService.getWaitingSessions();

        // Then
        assertThat(waitingSessions).hasSize(1);
        assertThat(waitingSessions.get(0).sessionId()).isEqualTo(1L);
        then(listOps).should().remove("chatbot:waiting:queue", 1, "2");
    }

    // ===== 세션 사용자 ID 조회 테스트 =====

    @Test
    @DisplayName("세션의 사용자 ID를 조회할 수 있다")
    void shouldGetUserIdBySessionId() {
        // Given
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        // When
        var userId = consultantService.getUserIdBySessionId(1L);

        // Then
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 세션의 사용자 ID 조회 시 예외가 발생한다")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Given
        given(sessionRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> consultantService.getUserIdBySessionId(999L))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ChatbotErrorCode.SESSION_NOT_FOUND);
    }

    // ===== 세션 메시지 조회 테스트 =====

    @Test
    @DisplayName("세션의 메시지 목록을 조회할 수 있다")
    void shouldGetSessionMessages() {
        // Given
        ChatMessage userMsg = ChatMessage.createUserMessage(session, "안녕");
        ChatMessage aiMsg = ChatMessage.createAIMessage(session, "반갑습니다");
        ReflectionTestUtils.setField(userMsg, "id", 1L);
        ReflectionTestUtils.setField(aiMsg, "id", 2L);

        given(messageRepository.findBySessionIdOrderBySentAtAsc(1L))
                .willReturn(List.of(userMsg, aiMsg));

        // When
        var messages = consultantService.getSessionMessages(1L);

        // Then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).senderName()).isEqualTo("테스트사용자");
        assertThat(messages.get(1).senderName()).isEqualTo("AI");
    }

    @Test
    @DisplayName("메시지가 없는 세션은 빈 목록을 반환한다")
    void shouldReturnEmptyMessagesForSessionWithNoMessages() {
        // Given
        given(messageRepository.findBySessionIdOrderBySentAtAsc(1L))
                .willReturn(new ArrayList<>());

        // When
        var messages = consultantService.getSessionMessages(1L);

        // Then
        assertThat(messages).isEmpty();
    }
}