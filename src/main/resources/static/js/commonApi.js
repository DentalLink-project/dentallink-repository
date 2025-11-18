// ===== Common API =====
// API 호출 관련 함수 (로그인, 회원가입, 토큰 갱신 등)

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
            document.getElementById('loginForm').reset();
            
            // 페이지별 로그인 후 처리
            if (typeof onLogin === 'function') {
                onLogin();
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
            document.getElementById('signupForm').reset();
        } else {
            showMessage(apiResponse.message || '회원가입에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [토큰 갱신] ---
async function refreshAccessToken() {
    const refreshToken = getRefreshToken();
    
    if (!refreshToken) {
        return false;
    }
    
    try {
        const response = await fetch(`${window.API_URL}/api/auth/refresh-token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken })
        });
        
        if (response.ok) {
            const authHeader = response.headers.get('Authorization');
            const newRefreshToken = response.headers.get('Refresh-Token');
            
            if (authHeader) {
                const token = authHeader.replace('Bearer ', '');
                setToken(token);
            }
            
            if (newRefreshToken) {
                localStorage.setItem('refreshToken', newRefreshToken);
            }
            
            return true;
        } else {
            // Refresh token도 만료됨
            removeToken();
            return false;
        }
    } catch (error) {
        console.error('Token refresh error:', error);
        removeToken();
        return false;
    }
}

// --- [API 호출 헬퍼 (401 자동 재시도)] ---
async function fetchWithAuth(url, options = {}) {
    const token = getToken();
    
    if (!token) {
        throw new Error('No token available');
    }
    
    // 첫 번째 시도
    options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
    
    let response = await fetch(url, options);
    
    // 401 오류 시 토큰 갱신 후 재시도
    if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        
        if (refreshed) {
            // 새 토큰으로 재시도
            const newToken = getToken();
            options.headers['Authorization'] = `Bearer ${newToken}`;
            response = await fetch(url, options);
        } else {
            // 갱신 실패 - 로그인 필요
            throw new Error('Authentication required');
        }
    }
    
    return response;
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

console.log('✅ CommonApi.js loaded');