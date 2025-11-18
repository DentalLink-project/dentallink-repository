// ===== My Page JavaScript =====

let currentUser = null;
let currentPage = 0;
let totalPages = 0;

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    // 로그인 체크
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => {
            window.location.href = '/';
        }, 1500);
        return;
    }

    // 사용자 정보 로드
    loadUserInfo();
    
    // 포인트 로그 로드
    loadPointLogs();

    // 비밀번호 변경 버튼
    const btnEditProfile = document.getElementById('btnEditProfile');
    if (btnEditProfile) {
        btnEditProfile.addEventListener('click', showPasswordModal);
    }

    // 회원 탈퇴 버튼
    const btnDeleteAccount = document.getElementById('btnDeleteAccount');
    if (btnDeleteAccount) {
        btnDeleteAccount.addEventListener('click', showDeleteModal);
    }

    // 포인트 충전 버튼
    const btnChargePoint = document.getElementById('btnChargePoint');
    if (btnChargePoint) {
        btnChargePoint.addEventListener('click', chargePoint);
    }

    // 날짜 필터
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');
    if (startDate) {
        startDate.addEventListener('change', () => {
            currentPage = 0;
            loadPointLogs();
        });
    }
    if (endDate) {
        endDate.addEventListener('change', () => {
            currentPage = 0;
            loadPointLogs();
        });
    }
});

// 사용자 정보 로드
async function loadUserInfo() {
    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/users/me`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            currentUser = apiResponse.data;
            displayUserInfo(currentUser);
            loadCurrentPoints();
        } else if (response.status === 401) {
            showMessage('로그인이 필요합니다.', 'error');
            setTimeout(() => showLoginModal(), 500);
        } else {
            showMessage('사용자 정보를 불러오는데 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Load user info error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// 사용자 정보 표시
function displayUserInfo(user) {
    document.getElementById('displayUsername').textContent = user.username || '사용자';
    document.getElementById('displayEmail').textContent = user.email || '-';
    
    if (user.createdAt) {
        const date = new Date(user.createdAt);
        const formatted = `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
        document.getElementById('displayCreatedAt').textContent = formatted;
    }
}

// 현재 포인트 로드
async function loadCurrentPoints() {
    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/point-logs/my?page=0&size=1`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            if (apiResponse.data && apiResponse.data.content && apiResponse.data.content.length > 0) {
                const latestLog = apiResponse.data.content[0];
                const currentPoints = latestLog.account.balance || 0;
                document.getElementById('currentPoints').textContent = `${currentPoints}P`;
            }
        }
    } catch (error) {
        console.error('Load current points error:', error);
    }
}

// 포인트 로그 로드
async function loadPointLogs() {
    try {
        const token = getToken();
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        
        let url = `${window.API_URL}/api/point-logs/my?page=${currentPage}&size=10&sort=latest`;
        
        if (startDateInput && startDateInput.value) {
            url += `&startDate=${startDateInput.value}T00:00:00`;
        }
        if (endDateInput && endDateInput.value) {
            url += `&endDate=${endDateInput.value}T23:59:59`;
        }

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            const pageData = apiResponse.data;
            
            totalPages = pageData.totalPages;
            displayPointLogs(pageData.content);
            displayLogPagination(pageData);
        } else {
            displayEmptyLogs();
        }
    } catch (error) {
        console.error('Load point logs error:', error);
        displayEmptyLogs();
    }
}

// 포인트 로그 표시
function displayPointLogs(logs) {
    const tbody = document.getElementById('pointLogBody');
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-row">포인트 로그가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => {
        const typeText = getPointTypeText(log.type);
        const typeClass = getPointTypeClass(log.type);
        const balanceBefore = log.account.balance - log.amount;
        
        return `
            <tr>
                <td><span class="${typeClass}">${typeText}</span></td>
                <td>${log.amount}P</td>
                <td>${balanceBefore}P</td>
                <td>${log.balanceAfter}P</td>
            </tr>
        `;
    }).join('');
}

// 빈 로그 표시
function displayEmptyLogs() {
    const tbody = document.getElementById('pointLogBody');
    tbody.innerHTML = '<tr><td colspan="4" class="empty-row">포인트 로그가 없습니다.</td></tr>';
}

// 로그 페이지네이션
function displayLogPagination(pageData) {
    const pagination = document.getElementById('logPagination');
    
    if (pageData.totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = `
        <button class="pagination-btn" onclick="changeLogPage(${currentPage - 1})" 
                ${currentPage === 0 ? 'disabled' : ''}>이전</button>
        <div class="pagination-numbers">
    `;

    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(pageData.totalPages - 1, currentPage + 2);

    if (startPage > 0) {
        html += `<button class="pagination-number" onclick="changeLogPage(0)">1</button>`;
        if (startPage > 1) html += '<span class="pagination-dots">...</span>';
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="pagination-number ${i === currentPage ? 'active' : ''}" 
                         onclick="changeLogPage(${i})">${i + 1}</button>`;
    }

    if (endPage < pageData.totalPages - 1) {
        if (endPage < pageData.totalPages - 2) html += '<span class="pagination-dots">...</span>';
        html += `<button class="pagination-number" onclick="changeLogPage(${pageData.totalPages - 1})">${pageData.totalPages}</button>`;
    }

    html += `
        </div>
        <button class="pagination-btn" onclick="changeLogPage(${currentPage + 1})" 
                ${currentPage >= pageData.totalPages - 1 ? 'disabled' : ''}>다음</button>
    `;

    pagination.innerHTML = html;
}

// 페이지 변경
function changeLogPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadPointLogs();
}

// 포인트 타입 텍스트
function getPointTypeText(type) {
    const typeMap = {
        'CHARGE': '포인트 충전',
        'USE': '포인트 사용',
        'REFUND': '포인트 환불',
        'REWARD': '포인트 적립'
    };
    return typeMap[type] || type;
}

// 포인트 타입 클래스
function getPointTypeClass(type) {
    const classMap = {
        'CHARGE': 'point-type-charge',
        'USE': 'point-type-use',
        'REFUND': 'point-type-refund',
        'REWARD': 'point-type-charge'
    };
    return classMap[type] || '';
}

// 비밀번호 변경 모달
function showPasswordModal() {
    document.getElementById('passwordModal').style.display = 'flex';
}

function closePasswordModal() {
    document.getElementById('passwordModal').style.display = 'none';
    document.getElementById('passwordForm').reset();
}

// 비밀번호 변경
async function changePassword(event) {
    event.preventDefault();

    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const newPasswordConfirm = document.getElementById('newPasswordConfirm').value;

    if (newPassword !== newPasswordConfirm) {
        showMessage('새 비밀번호가 일치하지 않습니다.', 'error');
        return;
    }

    const passwordValidation = validatePassword(newPassword);
    if (!passwordValidation.valid) {
        showMessage(passwordValidation.message, 'error');
        return;
    }

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/users/password`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                oldPassword: oldPassword,
                newPassword: newPassword
            })
        });

        if (response.ok) {
            showMessage('비밀번호가 변경되었습니다.', 'success');
            closePasswordModal();
        } else {
            const error = await response.json();
            showMessage(error.message || '비밀번호 변경에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Change password error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// 회원 탈퇴 모달
function showDeleteModal() {
    document.getElementById('deleteModal').style.display = 'flex';
}

function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    document.getElementById('deleteForm').reset();
}

// 회원 탈퇴
async function deleteAccount(event) {
    event.preventDefault();

    const password = document.getElementById('deletePassword').value;

    if (!confirm('정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        return;
    }

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/users/withdraw`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                password: password
            })
        });

        if (response.ok) {
            showMessage('회원 탈퇴가 완료되었습니다.', 'success');
            clearToken();
            clearUser();
            setTimeout(() => {
                window.location.href = '/';
            }, 1500);
        } else {
            const error = await response.json();
            showMessage(error.message || '회원 탈퇴에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Delete account error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// 포인트 충전 (토스 결제 연동)
function chargePoint() {
    // TODO: 토스 결제 연동
    showMessage('포인트 충전 기능은 준비 중입니다.', 'info');
    
    // 토스 결제 연동 예시
    /*
    const amount = 10000; // 충전할 금액
    const orderId = 'order_' + new Date().getTime();
    const orderName = '포인트 충전';
    
    // 토스 결제 페이지로 이동
    window.location.href = `${window.API_URL}/api/payment/request?amount=${amount}&orderId=${orderId}&orderName=${orderName}`;
    */
}

console.log('✅ My-Page.js loaded');
