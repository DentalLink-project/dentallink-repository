// ===== Common Utilities =====
// 기본 유틸리티 함수 (토큰 관리, 메시지, 유효성 검사 등)

// --- [전역 설정] ---
window.API_URL = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080'
    : 'https://www.dentallink.store';


// --- [토큰 관리] ---
function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
}

function removeToken() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

function getUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
}

function setUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
}

function getRefreshToken() {
    return localStorage.getItem('refreshToken');
}

// --- [로그인 상태 확인] ---
function isLoggedIn() {
    return !!getToken();
}

function requireLogin() {
    if (!isLoggedIn()) {
        showMessage('로그인이 필요합니다.', 'error');
        showLoginModal();
        return false;
    }
    return true;
}

// --- [역할 확인] ---
function isAdmin() {
    const user = getUser();
    return user && user.role === 'ROLE_ADMIN';
}

function isHospital() {
    const user = getUser();
    return user && user.role === 'ROLE_HOSPITAL';
}

function getUserRole() {
    const user = getUser();
    return user ? user.role : null;
}

// --- [메시지 표시] ---
function showMessage(message, type = 'success') {
    const messageBox = document.getElementById('messageBox');
    if (!messageBox) return;

    const messageEl = document.createElement('div');
    messageEl.className = `message ${type}`;
    messageEl.textContent = message;

    messageBox.innerHTML = '';
    messageBox.appendChild(messageEl);

    setTimeout(() => {
        messageEl.classList.add('removing');
        setTimeout(() => {
            messageBox.innerHTML = '';
        }, 500);
    }, 5000);
}

// --- [날짜 포맷] ---
function formatDate(dateString) {
    if (!dateString) return '';

    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}.${month}.${day}`;
}

// --- [비밀번호 유효성 검사] ---
function validatePassword(password) {
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);

    if (!hasUpperCase) {
        return { valid: false, message: '비밀번호에 영어 대문자를 포함해야 합니다.' };
    }
    if (!hasLowerCase) {
        return { valid: false, message: '비밀번호에 영어 소문자를 포함해야 합니다.' };
    }
    if (!hasNumber) {
        return { valid: false, message: '비밀번호에 숫자를 포함해야 합니다.' };
    }
    if (!hasSpecialChar) {
        return { valid: false, message: '비밀번호에 특수문자를 포함해야 합니다.' };
    }
    if (password.length < 8) {
        return { valid: false, message: '비밀번호는 최소 8자 이상이어야 합니다.' };
    }

    return { valid: true };
}

// --- [모달 제어] ---
function showLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.add('show');
    }
}

function closeLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.remove('show');
        document.getElementById('loginForm')?.reset();
    }
}

function showSignupModal() {
    closeLoginModal();
    const modal = document.getElementById('signupModal');
    if (modal) {
        modal.classList.add('show');
    }
}

function closeSignupModal() {
    const modal = document.getElementById('signupModal');
    if (modal) {
        modal.classList.remove('show');
        document.getElementById('signupForm')?.reset();
    }
}

// --- [UI 업데이트] ---
function updateUIAfterLogin(user) {
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');

    if (loginBtn) loginBtn.style.display = 'none';
    if (logoutBtn) logoutBtn.style.display = 'inline-block';

    // 네비게이션 업데이트 (역할 기반)
    updateNavigationByRole();
}

function updateUIAfterLogout() {
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');

    if (logoutBtn) logoutBtn.style.display = 'none';
    if (loginBtn) loginBtn.style.display = 'inline-block';

    // 네비게이션 업데이트 (역할 기반)
    updateNavigationByRole();
}

// --- [로그인] ---
async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch(`${window.API_URL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password })
        });

        if (response.ok) {
            const apiResponse = await response.json();

            const authHeader = response.headers.get('Authorization');
            const refreshHeader = response.headers.get('Refresh-Token');

            if (authHeader) {
                const token = authHeader.replace('Bearer ', '');
                setToken(token);
            }

            if (refreshHeader) {
                localStorage.setItem('refreshToken', refreshHeader);
            }

            const userData = apiResponse.data || apiResponse;
            setUser(userData);

            const displayName = userData.username || userData.name || '사용자';
            showMessage(`${displayName}님 환영합니다!`, 'success');
            closeLoginModal();
            updateUIAfterLogin(userData);

            // 페이지별 로그인 후 처리
            if (typeof onLoginSuccess === 'function') {
                onLoginSuccess(userData);
            }
        } else {
            const error = await response.json();
            showMessage(error.message || '로그인에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [회원가입] ---
async function handleSignup(event) {
    event.preventDefault();

    const email = document.getElementById('signupEmail').value;
    const username = document.getElementById('signupUsername').value;
    const password = document.getElementById('signupPassword').value;
    const passwordConfirm = document.getElementById('signupPasswordConfirm').value;

    if (password !== passwordConfirm) {
        showMessage('비밀번호가 일치하지 않습니다.', 'error');
        return;
    }

    const passwordValidation = validatePassword(password);
    if (!passwordValidation.valid) {
        showMessage(passwordValidation.message, 'error');
        return;
    }

    try {
        const response = await fetch(`${window.API_URL}/api/users/signup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, username, password })
        });

        const apiResponse = await response.json();

        if (response.ok && apiResponse.success) {
            showMessage(apiResponse.message || '회원가입이 완료되었습니다! 로그인해주세요.', 'success');
            closeSignupModal();
            showLoginModal();
        } else {
            showMessage(apiResponse.message || '회원가입에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [로그아웃] ---
async function handleLogout() {
    try {
        const token = getToken();

        if (token) {
            try {
                await fetch(`${window.API_URL}/api/auth/logout`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });
            } catch (logoutError) {
                console.warn('Logout API failed, but continuing with local cleanup:', logoutError);
            }
        }
    } finally {
        removeToken();
        showMessage('로그아웃되었습니다.', 'success');
        updateUIAfterLogout();

        // 페이지별 로그아웃 후 처리
        if (typeof onLogout === 'function') {
            onLogout();
        }
    }
}

// --- [네비게이션 업데이트 - 역할 기반] ---
function updateNavigationByRole() {
    const adminMenu = document.getElementById('adminChatMenu');
    if (!adminMenu) return;

    if (isAdmin()) {
        // 관리자: 메뉴 표시
        adminMenu.style.display = 'block';
    } else {
        // 관리자 아님: 메뉴 숨김
        adminMenu.style.display = 'none';
    }
}

// --- [페이지 로드 시 실행] ---
document.addEventListener('DOMContentLoaded', function() {
    // 로그인 상태 확인
    const user = getUser();
    if (user) {
        updateUIAfterLogin(user);
    }

    // 네비게이션 업데이트 (역할 기반)
    updateNavigationByRole();

    // 로그인 버튼 이벤트
    document.getElementById('loginBtn')?.addEventListener('click', showLoginModal);

    // 로그아웃 버튼 이벤트
    document.getElementById('logoutBtn')?.addEventListener('click', handleLogout);

    // 플로팅 메뉴 토글 (모바일)
    const floatingToggle = document.getElementById('floatingToggle');
    const floatingItems = document.getElementById('floatingItems');

    if (floatingToggle && floatingItems) {
        floatingToggle.addEventListener('click', function(e) {
            e.stopPropagation();
            floatingItems.classList.toggle('show');
            floatingToggle.textContent = floatingItems.classList.contains('show') ? '×' : '☰';
        });

        // 외부 클릭 시 메뉴 닫기
        document.addEventListener('click', function(e) {
            if (!e.target.closest('.floating-menu')) {
                floatingItems.classList.remove('show');
                floatingToggle.textContent = '☰';
            }
        });
    }
});

// --- [SVG 아이콘] ---
const SVGIcons = {
    starOutlined: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#5dade2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>`,
    starFilled: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="#5dade2" stroke="#5dade2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>`
};

console.log('✅ common.js loaded');