// ===== Admin Chat Page JavaScript - WebSocket Only =====

let stompClient = null;
let currentSessionId = null;
let currentUserId = null;  // ← 사용자 ID 추가
let currentConsultantId = null;
let isTyping = false;
let isConnected = false;
let waitingSessionsList = [];

// --- [페이지 로드 시 실행] ---
document.addEventListener('DOMContentLoaded', function() {
    // 관리자 권한 확인
    if (!isAdmin()) {
        showMessage('관리자만 접근 가능합니다.', 'error');
        setTimeout(() => {
            window.location.href = '/';
        }, 2000);
        return;
    }

    // 로그인 확인
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    // 현재 사용자 ID 저장
    const user = getUser();
    if (user) {
        currentConsultantId = user.userId;
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
                sendConsultantMessage();
            }
        });

        chatInput.addEventListener('focus', function() {
            adjustTextareaHeight(this);
        });
    }

    // WebSocket 연결
    connectWebSocket();

    // 대기 세션 목록 로드
    loadWaitingSessions();

    // 대기 세션 목록 주기적 새로고침 (3초)
    setInterval(loadWaitingSessions, 3000);
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
    console.log('WebSocket connected');
    isConnected = true;

    // 상담원이 수신할 메시지 채널 구독
    stompClient.subscribe('/user/queue/reply', onMessageReceived);
    console.log('Subscribed to /user/queue/reply');
}

function onError(error) {
    console.error('WebSocket connection error:', error);
    isConnected = false;
    showMessage('채팅 서버 연결에 실패했습니다.', 'error');

    // 5초 후 재연결 시도
    setTimeout(() => {
        if (getToken()) {
            console.log('Reconnecting WebSocket...');
            connectWebSocket();
        }
    }, 5000);
}

function onMessageReceived(message) {
    console.log('Message received:', message.body);

    try {
        const response = JSON.parse(message.body);

        // 타이핑 표시 제거
        hideTypingIndicator();

        // 입력창 활성화
        enableInput();

        // 메시지 표시
        if (response.content) {
            displayMessage(response.type ? response.type.toLowerCase() : 'user', response.content);
        }

    } catch (error) {
        console.error('Message parsing error:', error);
        hideTypingIndicator();
        enableInput();
        displayMessage('system', '메시지를 처리하는 중 오류가 발생했습니다.');
    }
}

// --- [대기 세션 목록 로드] ---
async function loadWaitingSessions() {
    const token = getToken();
    if (!token) return;

    try {
        const response = await fetch(`${window.API_URL}/api/consultant/waiting-sessions`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            const sessions = apiResponse.data || apiResponse || [];

            console.log('📋 Waiting sessions:', sessions);
            waitingSessionsList = sessions;

            // 대기 세션 목록 업데이트
            updateSessionList(sessions);
        }
    } catch (error) {
        console.error('Failed to load waiting sessions:', error);
    }
}

function updateSessionList(sessions) {
    const sessionList = document.getElementById('sessionList');
    const waitingCount = document.getElementById('waitingCount');

    if (!sessions || sessions.length === 0) {
        sessionList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">👥</div>
                <div class="empty-state-text">대기 중인 사용자가 없습니다.</div>
            </div>
        `;
        waitingCount.textContent = '0';
        return;
    }

    waitingCount.textContent = sessions.length;

    sessionList.innerHTML = '';
    sessions.forEach(session => {
        const sessionEl = document.createElement('div');
        sessionEl.className = `session-item ${currentSessionId === session.sessionId ? 'active' : ''}`;
        sessionEl.id = `session-${session.sessionId}`;

        const waitingTime = formatWaitingTime(session.startedAt);

        sessionEl.innerHTML = `
            <div class="session-info">
                <div class="session-user-name">${session.userName || '사용자'}</div>
                <div class="session-waiting-time">${waitingTime} 대기</div>
            </div>
            <div class="session-position">#${session.waitingPosition}</div>
        `;

        sessionEl.addEventListener('click', () => selectSession(session));
        sessionList.appendChild(sessionEl);
    });
}

// --- [세션 선택] ---
async function selectSession(session) {
    currentSessionId = session.sessionId;
    currentUserId = session.userId;  // ← 사용자 ID 저장
    const token = getToken();

    if (!token) return;

    try {
        // WebSocket으로 pick 메시지 전송 (세션 수락)
        if (isConnected && stompClient) {
            stompClient.send('/app/consultant/pick', {}, JSON.stringify({
                sessionId: currentSessionId
            }));
        }

        // 세션 메시지 로드
        await loadSessionMessages(currentSessionId);

        // UI 업데이트
        document.getElementById('chatUserName').textContent = session.userName || '사용자';
        document.getElementById('chatStatus').textContent = '상담 중';
        document.getElementById('chatInputContainer').style.display = 'block';
        document.getElementById('closeSessionBtn').style.display = 'inline-block';

        // 세션 아이템 활성화 표시
        document.querySelectorAll('.session-item').forEach(item => {
            item.classList.remove('active');
        });
        const activeSession = document.getElementById(`session-${currentSessionId}`);
        if (activeSession) {
            activeSession.classList.add('active');
        }

        showMessage('상담 세션이 시작되었습니다.', 'success');
    } catch (error) {
        console.error('Failed to select session:', error);
        showMessage('세션 선택에 실패했습니다.', 'error');
    }
}

// --- [세션 메시지 로드] ---
async function loadSessionMessages(sessionId) {
    const token = getToken();
    if (!token) return;

    try {
        const response = await fetch(`${window.API_URL}/api/chat/sessions/${sessionId}/messages`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            const messages = apiResponse.data || apiResponse || [];

            console.log('Session messages:', messages);

            // 채팅 메시지 영역 초기화
            const messagesContainer = document.getElementById('chatMessages');
            messagesContainer.innerHTML = '';

            // 메시지 표시
            messages.forEach(msg => {
                displayMessage(msg.type ? msg.type.toLowerCase() : 'user', msg.content);
            });
        }
    } catch (error) {
        console.error('Failed to load session messages:', error);
    }
}

// --- [상담원 메시지 전송] ---
function sendConsultantMessage() {
    const chatInput = document.getElementById('chatInput');
    const content = chatInput.value.trim();

    if (!content || isTyping || !currentSessionId) return;

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

    // 상담원 메시지 표시
    displayMessage('consultant', content);

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
            userId: currentUserId,  // ← userId 추가
            content: content
        };

        console.log('Sending consultant message:', messagePayload);

        stompClient.send('/app/consultant/send', {}, JSON.stringify(messagePayload));

        // 상담원 메시지는 백엔드에서 응답이 없으므로 즉시 입력창 활성화
        setTimeout(() => {
            hideTypingIndicator();
            enableInput();
        }, 500);

    } catch (error) {
        console.error('Send message error:', error);
        hideTypingIndicator();
        enableInput();
        showMessage('메시지 전송에 실패했습니다.', 'error');
    }
}

// --- [상담 종료] ---
async function closeConsultantSession() {
    if (!currentSessionId) return;

    const token = getToken();
    if (!token) return;

    if (!confirm('상담을 종료하시겠습니까?')) {
        return;
    }

    try {
        // WebSocket으로 close 메시지 전송
        if (isConnected && stompClient) {
            stompClient.send('/app/consultant/close', {}, JSON.stringify(currentSessionId));
        }

        // 상담 종료 API 호출
        await fetch(`${window.API_URL}/api/chat/sessions/${currentSessionId}/close`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }).catch(err => console.warn('Close API call optional:', err));

        // UI 초기화
        currentSessionId = null;
        document.getElementById('chatMessages').innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">💬</div>
                <div class="empty-state-text">
                    대기 중인 사용자를 선택하여<br>상담을 시작하세요.
                </div>
            </div>
        `;
        document.getElementById('chatUserName').textContent = '사용자 선택';
        document.getElementById('chatStatus').textContent = '대기 중';
        document.getElementById('chatInputContainer').style.display = 'none';
        document.getElementById('closeSessionBtn').style.display = 'none';

        // 세션 아이템 비활성화
        document.querySelectorAll('.session-item').forEach(item => {
            item.classList.remove('active');
        });

        showMessage('상담이 종료되었습니다.', 'success');

        // 대기 세션 목록 새로고침
        loadWaitingSessions();
    } catch (error) {
        console.error('Failed to close session:', error);
        showMessage('상담 종료에 실패했습니다.', 'error');
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

// --- [시간 포맷] ---
function formatTime(date) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
}

// --- [대기 시간 포맷] ---
function formatWaitingTime(startedAt) {
    const start = new Date(startedAt);
    const now = new Date();
    const diffMs = now - start;
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);

    if (diffMinutes < 1) {
        return '방금';
    } else if (diffMinutes < 60) {
        return `${diffMinutes}분`;
    } else {
        const hours = Math.floor(diffMinutes / 60);
        return `${hours}시간`;
    }
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
        chatInput.placeholder = '메시지 전송 중...';
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
        console.log('🔌 WebSocket disconnected');
    }
}

// --- [로그인 성공 후 처리] ---
function onLoginSuccess(user) {
    currentConsultantId = user.userId;
    loadWaitingSessions();
    connectWebSocket();
}

// --- [로그아웃 후 처리] ---
function onLogout() {
    disconnectWebSocket();
    currentSessionId = null;
    currentConsultantId = null;
}

// --- [페이지 언로드 시] ---
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});

// --- [상담 종료 버튼 이벤트] ---
function setupCloseSessionButton() {
    const closeSessionBtn = document.getElementById('closeSessionBtn');
    if (closeSessionBtn) {
        closeSessionBtn.addEventListener('click', closeConsultantSession);
    }
}

// DOMContentLoaded 이벤트가 이미 발생한 경우를 대비
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupCloseSessionButton);
} else {
    setupCloseSessionButton();
}

console.log('Admin-chat.js - WebSocket Only Mode loaded');