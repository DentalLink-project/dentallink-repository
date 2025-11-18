// ===== Chatbot Page JavaScript - WebSocket Only =====

let stompClient = null;
let currentSessionId = null;
let isTyping = false;
let isConnected = false;

// --- [페이지 로드 시 실행] ---
document.addEventListener('DOMContentLoaded', function() {
    // 로그인 확인
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    // textarea 자동 높이 조절
    const chatInput = document.getElementById('chatInput');
    if (chatInput) {
        chatInput.addEventListener('input', function() {
            adjustTextareaHeight(this);
            updateCharCount();
        });

        // Enter 키로 전송 (Shift+Enter는 줄바꿈)
        chatInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        chatInput.addEventListener('focus', function() {
            adjustTextareaHeight(this);
        });
    }

    // 초기 메시지 표시
    displayWelcomeMessage();

    // WebSocket 연결
    connectWebSocket();
});

// --- [WebSocket 연결] ---
function connectWebSocket() {
    const token = getToken();
    if (!token) {
        console.log('No token available');
        return;
    }

    // 네이티브 WebSocket 사용
    const wsUrl = window.location.protocol === 'https:'
        ? `wss://${window.location.host}/ws/chat`
        : `ws://${window.location.host}/ws/chat`;

    const ws = new WebSocket(wsUrl);
    stompClient = Stomp.over(ws);

    // 디버그 로그 활성화
    stompClient.debug = function(str) {
        console.log('STOMP: ' + str);
    };

    const headers = {
        'Authorization': `Bearer ${token}`
    };

    stompClient.connect(headers, onConnected, onError);
}

function onConnected() {
    console.log('✅ WebSocket connected');
    isConnected = true;
    updateStatusIndicator('connected');

    // 사용자별 메시지 구독
    stompClient.subscribe('/user/queue/reply', onMessageReceived);
    console.log('📡 Subscribed to /user/queue/reply');
}

function onError(error) {
    console.error('❌ WebSocket connection error:', error);
    isConnected = false;
    updateStatusIndicator('offline');
    showMessage('채팅 서버 연결에 실패했습니다.', 'error');

    // 5초 후 재연결 시도
    setTimeout(() => {
        if (getToken()) {
            console.log('🔄 Reconnecting WebSocket...');
            connectWebSocket();
        }
    }, 5000);
}

function onMessageReceived(message) {
    console.log('📨 Message received:', message.body);

    try {
        const response = JSON.parse(message.body);

        // 타이핑 표시 제거
        hideTypingIndicator();

        // 입력창 활성화
        enableInput();

        // 세션 ID 저장
        if (response.sessionId && !currentSessionId) {
            currentSessionId = response.sessionId;
            console.log('📝 Session ID:', currentSessionId);
        }

        // 에러 체크 (content에 "오류" 또는 "실패" 포함 시)
        if (response.content && (response.content.includes('오류') || response.content.includes('실패'))) {
            console.error('❌ Error in response:', response.content);
            displayMessage('system', response.content);
        } else {
            // 정상 메시지 표시
            displayMessage(response.type ? response.type.toLowerCase() : 'ai', response.content);
        }

        // 상태 업데이트
        if (response.type === 'SYSTEM') {
            updateStatusToWaiting();
        } else if (response.type === 'CONSULTANT') {
            updateStatusToConsultant();
        }

    } catch (error) {
        console.error('Message parsing error:', error);
        hideTypingIndicator();
        enableInput();
        displayMessage('system', '메시지를 처리하는 중 오류가 발생했습니다.');
    }
}

// --- [환영 메시지 표시] ---
function displayWelcomeMessage() {
    const messagesContainer = document.getElementById('chatMessages');
    messagesContainer.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">💬</div>
            <div class="empty-state-text">
                안녕하세요! DentalLink AI 상담사입니다.<br>
                무엇을 도와드릴까요?
            </div>
        </div>
    `;
}

// --- [메시지 전송 - WebSocket Only] ---
function sendMessage() {
    const chatInput = document.getElementById('chatInput');
    const content = chatInput.value.trim();

    if (!content || isTyping) return;

    // 로그인 확인
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    // WebSocket 연결 확인
    if (!isConnected || !stompClient) {
        showMessage('채팅 서버에 연결 중입니다...', 'info');
        connectWebSocket();
        return;
    }

    // 사용자 메시지 표시
    displayMessage('user', content);

    // 입력창 비활성화
    disableInput();

    // 입력창 초기화
    chatInput.value = '';
    adjustTextareaHeight(chatInput);
    updateCharCount();

    // 타이핑 표시
    showTypingIndicator();

    try {
        // WebSocket으로 메시지 전송
        const messagePayload = {
            sessionId: currentSessionId,
            content: content
        };

        console.log('📤 Sending message:', messagePayload);

        stompClient.send('/app/chat/send', {}, JSON.stringify(messagePayload));

    } catch (error) {
        console.error('Send message error:', error);
        hideTypingIndicator();
        enableInput();
        showMessage('메시지 전송에 실패했습니다.', 'error');
    }
}

// --- [메시지 표시] ---
function displayMessage(type, content) {
    const messagesContainer = document.getElementById('chatMessages');

    // 환영 메시지 제거
    const emptyState = messagesContainer.querySelector('.empty-state');
    if (emptyState) {
        emptyState.remove();
    }

    const messageEl = document.createElement('div');
    messageEl.className = `message-bubble ${type}`;

    const contentEl = document.createElement('div');
    contentEl.className = 'message-content';
    contentEl.textContent = content;

    const timeEl = document.createElement('div');
    timeEl.className = 'message-time';
    timeEl.textContent = formatTime(new Date());

    messageEl.appendChild(contentEl);
    messageEl.appendChild(timeEl);
    messagesContainer.appendChild(messageEl);

    // 스크롤을 최하단으로
    scrollToBottom();
}

// --- [타이핑 표시] ---
function showTypingIndicator() {
    isTyping = true;
    const messagesContainer = document.getElementById('chatMessages');

    // 기존 타이핑 표시 제거
    const existingTyping = document.getElementById('typingIndicator');
    if (existingTyping) return;

    const typingEl = document.createElement('div');
    typingEl.className = 'typing-indicator';
    typingEl.id = 'typingIndicator';
    typingEl.innerHTML = `
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
    `;

    messagesContainer.appendChild(typingEl);
    scrollToBottom();
}

function hideTypingIndicator() {
    isTyping = false;
    const typingIndicator = document.getElementById('typingIndicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

// --- [상태 업데이트] ---
function updateStatusIndicator(status) {
    const statusIndicator = document.getElementById('statusIndicator');
    if (statusIndicator) {
        statusIndicator.className = `status-indicator ${status}`;
    }
}

function updateStatusToWaiting() {
    updateStatusIndicator('waiting');
    const statusText = document.getElementById('statusText');
    if (statusText) {
        statusText.textContent = '상담원 대기중';
    }
}

function updateStatusToConsultant() {
    updateStatusIndicator('connected');
    const statusText = document.getElementById('statusText');
    if (statusText) {
        statusText.textContent = '상담원 연결됨';
    }
}

function updateStatusToAI() {
    updateStatusIndicator('connected');
    const statusText = document.getElementById('statusText');
    if (statusText) {
        statusText.textContent = 'AI 상담사';
    }
}

// --- [시간 포맷] ---
function formatTime(date) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
}

// --- [스크롤 최하단으로] ---
function scrollToBottom() {
    const messagesContainer = document.getElementById('chatMessages');
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// --- [Textarea 자동 높이 조절] ---
function adjustTextareaHeight(textarea) {
    textarea.style.height = '50px';

    const scrollHeight = textarea.scrollHeight;
    const newHeight = Math.min(Math.max(scrollHeight, 50), 150);

    textarea.style.height = newHeight + 'px';

    if (scrollHeight > 150) {
        textarea.style.overflowY = 'auto';
    } else {
        textarea.style.overflowY = 'hidden';
    }
}

// --- [글자 수 업데이트] ---
function updateCharCount() {
    const chatInput = document.getElementById('chatInput');
    const charCount = document.getElementById('charCount');

    if (chatInput && charCount) {
        const count = chatInput.value.length;
        charCount.textContent = `${count}/2000`;

        if (count > 1900) {
            charCount.style.color = '#e74c3c';
        } else {
            charCount.style.color = '#999';
        }
    }
}

// --- [입력 활성화/비활성화] ---
function disableInput() {
    const chatInput = document.getElementById('chatInput');
    const sendBtn = document.getElementById('sendBtn');

    if (chatInput) {
        chatInput.disabled = true;
        chatInput.placeholder = 'AI가 답변 중입니다...';
    }
    if (sendBtn) {
        sendBtn.disabled = true;
    }
}

function enableInput() {
    const chatInput = document.getElementById('chatInput');
    const sendBtn = document.getElementById('sendBtn');

    if (chatInput) {
        chatInput.disabled = false;
        chatInput.placeholder = '메시지를 입력하세요 (Enter: 전송, Shift+Enter: 줄바꿈)';
    }
    if (sendBtn) {
        sendBtn.disabled = false;
    }
}

// --- [WebSocket 연결 해제] ---
function disconnectWebSocket() {
    if (stompClient !== null && isConnected) {
        stompClient.disconnect();
        isConnected = false;
        updateStatusIndicator('offline');
        console.log('🔌 WebSocket disconnected');
    }
}

// --- [로그인 성공 후 처리] ---
function onLoginSuccess(user) {
    displayWelcomeMessage();
    connectWebSocket();
}

// --- [로그아웃 후 처리] ---
function onLogout() {
    disconnectWebSocket();
    currentSessionId = null;
    displayWelcomeMessage();
}

// --- [페이지 언로드 시] ---
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});

console.log('✅ Chatbot.js - WebSocket Only Mode loaded');