// ===== Common Animation =====
// UI 애니메이션 및 인터랙션 (모달, 플로팅 메뉴 등)

// --- [모달 제어] ---
function showLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.add('show');
        modal.style.display = 'flex';
    }
}

function closeLoginModal() {
    const modal = document.getElementById('loginModal');
    if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
    }
}

function showSignupModal() {
    closeLoginModal();
    const modal = document.getElementById('signupModal');
    if (modal) {
        modal.classList.add('show');
        modal.style.display = 'flex';
    }
}

function closeSignupModal() {
    const modal = document.getElementById('signupModal');
    if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
    }
}

// --- [UI 업데이트] ---
function updateUIAfterLogin(user) {
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    
    if (loginBtn) loginBtn.style.display = 'none';
    if (logoutBtn) logoutBtn.style.display = 'inline-block';
}

function updateUIAfterLogout() {
    const loginBtn = document.getElementById('loginBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    
    if (logoutBtn) logoutBtn.style.display = 'none';
    if (loginBtn) loginBtn.style.display = 'inline-block';
}

// --- [플로팅 메뉴 초기화] ---
function initFloatingMenu() {
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
}

// --- [페이지 로드 시 실행] ---
document.addEventListener('DOMContentLoaded', function() {
    // 로그인 상태 확인
    const user = getUser();
    if (user) {
        updateUIAfterLogin(user);
    }
    
    // 로그인 버튼 이벤트
    document.getElementById('loginBtn')?.addEventListener('click', showLoginModal);
    
    // 로그아웃 버튼 이벤트
    document.getElementById('logoutBtn')?.addEventListener('click', handleLogout);
    
    // 플로팅 메뉴 초기화
    initFloatingMenu();
});

console.log('CommonAnimation.js loaded');