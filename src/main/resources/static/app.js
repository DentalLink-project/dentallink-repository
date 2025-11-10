// Global State
let currentUser = null;
let hospitals = [];
let hospitalsPageNo = 0;
let hospitalsPageSize = 100;
let hospitalsTotalPages = 1;
let hospitalsSearchQuery = '';
let currentHospital = null;
let reservations = [];

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    // Check if user is logged in (auto-login)
    if (authToken) {
        loadUserProfile();
        updateNavbar();
    } else {
        updateNavbar();
    }

    // Show home page by default
    navigateTo('home');

    // Add Enter key support for forms
    addFormEnterKeySupport();
});

// Navigation Functions
function navigateTo(page) {
    // 현재 상담원 대시보드에서 벗어나는 경우 정리
    const currentPage = document.querySelector('.page.active');
    if (currentPage && currentPage.id === 'consultantDashboard') {
        cleanupConsultantDashboard();
    }

    // Hide all pages
    document.querySelectorAll('.page').forEach(p => {
        p.classList.remove('active');
    });

    // Show target page
    const targetPage = document.getElementById(page);
    if (targetPage) {
        targetPage.classList.add('active');

        // Load page-specific data
        if (page === 'hospitals') {
            loadHospitals();
        } else if (page === 'reservations') {
            if (authToken) {
                loadReservations();
            } else {
                showAlert('로그인이 필요합니다', 'info');
                navigateTo('login');
            }
        } else if (page === 'points') {
            if (authToken) {
                loadPoints();
            } else {
                showAlert('로그인이 필요합니다', 'info');
                navigateTo('login');
            }
        } else if (page === 'hospitalReservations') {
            if (authToken && currentUser && currentUser.userRole === 'HOSPITAL') {
                loadHospitalReservations();
            } else {
                showAlert('병원 관리자만 접근할 수 있습니다', 'error');
                navigateTo('home');
            }
        } else if (page === 'chatbot') {
            // Initialize chatbot page
            setTimeout(() => {
                initChatPage();
            }, 100);
        } else if (page === 'consultantDashboard') {
            if (authToken && currentUser && (currentUser.userRole && String(currentUser.userRole).includes('ADMIN'))) {
                setTimeout(() => {
                    initConsultantDashboard();
                }, 100);
            } else {
                showAlert('관리자만 접근할 수 있습니다', 'error');
                navigateTo('home');
            }
        }
    }

    window.scrollTo(0, 0);
}

// User Functions
async function loadUserProfile() {
    try {
        currentUser = await authAPI.getProfile();
        updateNavbar();
    } catch (error) {
        console.error('Failed to load profile:', error);
    }
}

function updateNavbar() {
    const loginMenu = document.getElementById('loginMenu');
    const logoutMenu = document.getElementById('logoutMenu');
    const customerMenu = document.getElementById('customerMenu');
    const customerReservations = document.getElementById('customerReservations');
    const hospitalMenu = document.getElementById('hospitalMenu');
    const adminMenu = document.getElementById('adminMenu');
    const adminFab = document.getElementById('adminFab');

    if (authToken && currentUser) {
        if (loginMenu) loginMenu.style.display = 'none';
        if (logoutMenu) logoutMenu.style.display = 'block';

        // Show/hide menus based on user role
        if (currentUser.userRole && String(currentUser.userRole).includes('HOSPITAL')) {
            if (customerMenu) customerMenu.style.display = 'none';
            if (customerReservations) customerReservations.style.display = 'none';
            if (hospitalMenu) hospitalMenu.style.display = 'block';
            if (adminMenu) adminMenu.style.display = 'none';
            if (adminFab) adminFab.style.display = 'none';
        } else if (currentUser.userRole && String(currentUser.userRole).includes('ADMIN')) {
            if (customerMenu) customerMenu.style.display = 'block';
            if (customerReservations) customerReservations.style.display = 'block';
            if (hospitalMenu) hospitalMenu.style.display = 'none';
            if (adminMenu) adminMenu.style.display = 'block';
            if (adminFab) adminFab.style.display = 'block';
        } else {
            if (customerMenu) customerMenu.style.display = 'block';
            if (customerReservations) customerReservations.style.display = 'block';
            if (hospitalMenu) hospitalMenu.style.display = 'none';
            if (adminMenu) adminMenu.style.display = 'none';
            if (adminFab) adminFab.style.display = 'none';
        }
    } else {
        if (loginMenu) loginMenu.style.display = 'block';
        if (logoutMenu) logoutMenu.style.display = 'none';
        if (customerMenu) customerMenu.style.display = 'block';
        if (customerReservations) customerReservations.style.display = 'block';
        if (hospitalMenu) hospitalMenu.style.display = 'none';
        if (adminMenu) adminMenu.style.display = 'none';
        if (adminFab) adminFab.style.display = 'none';
    }
}

// Auth Functions
async function handleLogin(event) {
    event.preventDefault();

    const loginForm = document.getElementById('loginForm');
    const submitButton = loginForm.querySelector('button[type="submit"]');
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    // Validate inputs
    if (!email || !password) {
        showAlert('이메일과 비밀번호를 입력해주세요', 'error');
        return;
    }

    if (!email.includes('@')) {
        showAlert('올바른 이메일 형식을 입력해주세요', 'error');
        return;
    }

    // Disable button to prevent double submission
    const originalText = submitButton.textContent;
    submitButton.disabled = true;
    submitButton.textContent = '로그인 중...';

    try {
        await authAPI.login(email, password);
        await loadUserProfile();

        showAlert('로그인 성공! 홈페이지로 이동합니다.', 'success');
        loginForm.reset();
        navigateTo('home');
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 401) {
                showAlert('이메일 또는 비밀번호가 올바르지 않습니다', 'error');
            } else if (error.status === 400) {
                showAlert('입력한 정보가 올바르지 않습니다', 'error');
            } else {
                showAlert(error.message, 'error');
            }
        } else {
            showAlert('로그인 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Login error:', error);
    } finally {
        // Re-enable button
        submitButton.disabled = false;
        submitButton.textContent = originalText;
    }
}

async function handleSignup(event) {
    event.preventDefault();

    const signupForm = document.getElementById('signupForm');
    const submitButton = signupForm.querySelector('button[type="submit"]');
    const email = document.getElementById('signupEmail').value.trim();
    const username = document.getElementById('signupUsername').value.trim();
    const password = document.getElementById('signupPassword').value;
    const userRole = document.getElementById('signupRole').value;

    // Validate inputs
    if (!email || !username || !password) {
        showAlert('모든 필드를 입력해주세요', 'error');
        return;
    }

    if (!email.includes('@')) {
        showAlert('올바른 이메일 형식을 입력해주세요', 'error');
        return;
    }

    if (password.length < 6) {
        showAlert('비밀번호는 6자 이상이어야 합니다', 'error');
        return;
    }

    if (username.length < 2) {
        showAlert('사용자명은 2자 이상이어야 합니다', 'error');
        return;
    }

    // Disable button to prevent double submission
    const originalText = submitButton.textContent;
    submitButton.disabled = true;
    submitButton.textContent = '회원가입 중...';

    try {
        await authAPI.signup(email, username, password, userRole);

        showAlert('회원가입 성공! 로그인 페이지로 이동합니다.', 'success');
        signupForm.reset();
        navigateTo('login');
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 409) {
                showAlert('이미 존재하는 이메일입니다', 'error');
            } else if (error.status === 400) {
                showAlert('입력한 정보가 올바르지 않습니다', 'error');
            } else {
                showAlert(error.message, 'error');
            }
        } else {
            showAlert('회원가입 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Signup error:', error);
    } finally {
        // Re-enable button
        submitButton.disabled = false;
        submitButton.textContent = originalText;
    }
}

async function logout() {
    try {
        // 1. 채팅 페이지의 WebSocket 연결 종료
        if (chatbotStompClient) {
            console.log('채팅 페이지 WebSocket 연결 종료');
            chatbotStompClient.disconnect();
            chatbotStompClient = null;
            chatbotConnected = false;
            chatbotSessionId = null;
        }

        // 2. FAB 챗봇의 WebSocket 연결 종료
        if (window.fabChatStompClient) {
            console.log('FAB 챗봇 WebSocket 연결 종료');
            window.fabChatStompClient.disconnect();
            window.fabChatStompClient = null;
            window.fabChatConnected = false;
            window.fabChatSessionId = null;
        }

        // 3. 상담원 대시보드의 WebSocket 연결 종료
        if (consultantStompClient && consultantConnected) {
            console.log('상담원 WebSocket 연결 종료');
            try {
                consultantStompClient.disconnect(() => {});
            } catch (e) {
                console.error('Consultant WebSocket 종료 중 오류:', e);
            }
            consultantStompClient = null;
            consultantConnected = false;
            currentSessionId = null;
        }

        // 4. 모든 채팅 메시지 컨테이너 초기화 (일반 채팅, FAB 채팅, 상담원 채팅)
        const chatContainers = [
            'chat-messages',      // 채팅 페이지
            'chatMessages',       // FAB 챗봇
            'consultant-chat-messages' // 상담원 대시보드
        ];

        chatContainers.forEach(id => {
            const container = document.getElementById(id);
            if (container) {
                container.innerHTML = ''; // 모든 이전 메시지 제거
            }
        });

        // 5. 채팅 상태 변수 초기화
        window.chatbotFabInitialized = false; // FAB 초기화 상태 리셋
        isUserScrolling = false;
        hasNewMessages = false;
        isSendingChatMessage = false;
        waitingSessions = [];
        activeSessions = [];

        // 6. 상담원 대시보드 타이머 정리
        if (consultantSessionsIntervalId) {
            clearInterval(consultantSessionsIntervalId);
            consultantSessionsIntervalId = null;
        }

        // 7. 백엔드 로그아웃 API 호출
        await authAPI.logout();

        // 8. 프론트엔드 상태 초기화
        currentUser = null;
        updateNavbar();

        showAlert('로그아웃 되었습니다', 'success');
        navigateTo('home');
    } catch (error) {
        console.error('Logout error:', error);
        // 에러가 발생해도 UI는 초기화
        currentUser = null;
        updateNavbar();
        navigateTo('home');
    }
}

// Hospital Functions
async function loadHospitals(page = 0) {
    const hospitalsList = document.getElementById('hospitalsList');

    // Show skeleton loading cards
    showHospitalsSkeletonLoading();

    try {
        hospitalsPageNo = page;
        let data;
        if (hospitalsSearchQuery) {
            data = await hospitalsAPI.search(hospitalsSearchQuery, hospitalsPageNo, hospitalsPageSize);
        } else {
            data = await hospitalsAPI.getAll(hospitalsPageNo, hospitalsPageSize);
        }

        if (Array.isArray(data)) {
            hospitals = data;
            hospitalsTotalPages = 1;
        } else {
            hospitals = data.content || [];
            hospitalsTotalPages = typeof data.totalPages === 'number' ? data.totalPages : 1;
            hospitalsPageNo = typeof data.number === 'number' ? data.number : hospitalsPageNo;
        }

        renderHospitals(hospitals);
        renderHospitalsPagination();
    } catch (error) {
        console.error('Failed to load hospitals:', error);

        // Show error with retry button
        if (error instanceof APIError) {
            hospitalsList.innerHTML = `
                <div class="empty-state">
                    <p>병원을 불러올 수 없습니다</p>
                    <p style="color: #666; font-size: 0.9rem; margin: 1rem 0;">
                        ${error.message}
                    </p>
                    <button class="btn btn-primary" style="width: auto; margin-top: 1rem;" onclick="loadHospitals(0)">
                        다시 시도
                    </button>
                </div>
            `;
        } else {
            hospitalsList.innerHTML = `
                <div class="empty-state">
                    <p>네트워크 오류가 발생했습니다</p>
                    <p style="color: #666; font-size: 0.9rem; margin: 1rem 0;">
                        인터넷 연결을 확인하고 다시 시도해주세요
                    </p>
                    <button class="btn btn-primary" style="width: auto; margin-top: 1rem;" onclick="loadHospitals(0)">
                        다시 시도
                    </button>
                </div>
            `;
        }
    }
}

/**
 * Show skeleton loading cards (3개)
 */
function showHospitalsSkeletonLoading() {
    const hospitalsList = document.getElementById('hospitalsList');
    let skeletonHTML = '';

    for (let i = 0; i < 3; i++) {
        skeletonHTML += `
            <div class="skeleton-card">
                <div class="skeleton-text title"></div>
                <div class="skeleton-text content"></div>
                <div class="skeleton-text content"></div>
                <div class="skeleton-text content"></div>
                <div class="skeleton-text button"></div>
            </div>
        `;
    }

    hospitalsList.innerHTML = skeletonHTML;
}

function renderHospitals(hospitalsList) {
    const container = document.getElementById('hospitalsList');

    if (!hospitalsList || hospitalsList.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>등록된 병원이 없습니다.</p></div>';
        return;
    }

    container.innerHTML = hospitalsList.map(hospital => `
        <div class="hospital-card" onclick="viewHospitalDetail(${hospital.id})">
            <div class="hospital-card-body">
                <h3>${hospital.hospitalName || '병원 이름'}</h3>
                <p>👨‍⚕️ ${hospital.doctorName || '의사 정보 없음'}</p>
                <p>${hospital.hospitalIsOpen ? '✅ 영업 중' : '❌ 영업 종료'}</p>
            </div>
            <div class="hospital-card-footer">
                <button class="btn btn-primary" onclick="viewHospitalDetail(${hospital.id})">자세히 보기</button>
            </div>
        </div>
    `).join('');
}

let hospitalsSearchTimer = null;

/**
 * 병원 검색 함수 (500ms 디바운스)
 */
function searchHospitals() {
    const searchInput = document.getElementById('searchInput');
    const query = (searchInput.value || '').trim();
    const container = document.getElementById('hospitalsList');
    const clearBtn = document.getElementById('searchClearBtn');

    // 검색어가 없으면 전체 목록 표시 및 버튼 숨기기
    if (!query) {
        hospitalsSearchQuery = '';
        if (clearBtn) clearBtn.style.display = 'none';
        loadHospitals(0);
        return;
    }

    // 검색어 있으면 클리어 버튼 표시
    if (clearBtn) clearBtn.style.display = 'inline-block';

    // 로딩 상태 표시
    if (container) {
        container.innerHTML = `
            <div class="loading">
                <div class="spinner"></div>
                <p style="margin-top: 1rem; color: #666;">검색 중...</p>
            </div>
        `;
    }

    // 기존 타이머 취소
    if (hospitalsSearchTimer) clearTimeout(hospitalsSearchTimer);

    // 500ms 디바운스 적용
    hospitalsSearchTimer = setTimeout(async () => {
        try {
            hospitalsSearchQuery = query;
            await loadHospitals(0);

            // 검색 결과가 없으면 메시지 표시
            if (hospitals.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <p>검색 결과가 없습니다</p>
                        <p style="color: #666; font-size: 0.9rem; margin: 1rem 0;">
                            "${query}"에 대한 병원을 찾을 수 없습니다
                        </p>
                    </div>
                `;
            }
        } catch (e) {
            console.error('Search failed:', e);
            // 실패 시 클라이언트 필터로 대체 (폴백)
            const fallback = hospitals.filter(h =>
                (h.hospitalName && h.hospitalName.toLowerCase().includes(query.toLowerCase())) ||
                (h.address && h.address.toLowerCase().includes(query.toLowerCase())) ||
                (h.doctorName && h.doctorName.toLowerCase().includes(query.toLowerCase()))
            );

            if (fallback.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <p>검색 결과가 없습니다</p>
                        <p style="color: #666; font-size: 0.9rem; margin: 1rem 0;">
                            로컬 검색에서도 "${query}"에 대한 병원을 찾을 수 없습니다
                        </p>
                    </div>
                `;
            } else {
                renderHospitals(fallback);
                renderHospitalsPagination();
            }
        }
    }, 500);  // 500ms 디바운스
}

/**
 * 검색 입력 필드 초기화 함수
 */
function clearSearch() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('searchClearBtn');

    searchInput.value = '';
    if (clearBtn) clearBtn.style.display = 'none';

    hospitalsSearchQuery = '';
    loadHospitals(0);
    searchInput.focus();
}

function renderHospitalsPagination() {
    const pagination = document.getElementById('hospitalsPagination');
    if (!pagination) return;

    if (hospitalsTotalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    const buttons = [];
    // Prev
    buttons.push(`<button ${hospitalsPageNo === 0 ? 'disabled' : ''} onclick="goToHospitalsPage(${hospitalsPageNo - 1})">이전</button>`);

    // Page numbers (windowed)
    const windowSize = 5;
    const start = Math.max(0, hospitalsPageNo - Math.floor(windowSize / 2));
    const end = Math.min(hospitalsTotalPages - 1, start + windowSize - 1);
    for (let i = start; i <= end; i++) {
        buttons.push(`<button class="${i === hospitalsPageNo ? 'active' : ''}" onclick="goToHospitalsPage(${i})">${i + 1}</button>`);
    }

    // Next
    buttons.push(`<button ${hospitalsPageNo >= hospitalsTotalPages - 1 ? 'disabled' : ''} onclick="goToHospitalsPage(${hospitalsPageNo + 1})">다음</button>`);

    pagination.innerHTML = buttons.join('');
}

function goToHospitalsPage(page) {
    if (page < 0 || page >= hospitalsTotalPages) return;

    // Smooth scroll to hospital list before loading
    const hospitalsList = document.getElementById('hospitalsList');
    if (hospitalsList) {
        hospitalsList.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    loadHospitals(page);
}

// Flag to prevent duplicate reservation creation
let isLoadingAvailableSlots = false;

async function viewHospitalDetail(hospitalId) {
    try {
        currentHospital = await hospitalsAPI.getById(hospitalId);

        const content = document.getElementById('hospitalDetailContent');
        const schedule = currentHospital.schedule || {};

        content.innerHTML = `
            <div class="hospital-detail">
                <h2>${currentHospital.hospitalName || '병원'}</h2>

                <div class="detail-section">
                    <h3>병원 정보</h3>
                    <div class="detail-info">
                        <div class="info-item">
                            <strong>주소</strong>
                            ${currentHospital.address || '주소 없음'}
                        </div>
                        <div class="info-item">
                            <strong>의료진</strong>
                            ${currentHospital.doctorName || '정보 없음'}
                        </div>
                        <div class="info-item">
                            <strong>상태</strong>
                            ${currentHospital.hospitalIsOpen ? '✅ 영업 중' : '❌ 영업 종료'}
                        </div>
                    </div>
                    <p>${currentHospital.description || '설명이 없습니다'}</p>
                </div>

                <div class="detail-section">
                    <h3>진료 시간</h3>
                    <div class="detail-info">
                        <div class="info-item">
                            <strong>월-금</strong>
                            ${currentHospital.openTime && currentHospital.closeTime ? `${currentHospital.openTime} ~ ${currentHospital.closeTime}` : (schedule.mondayToFriday || '정보 없음')}
                        </div>
                        <div class="info-item">
                            <strong>점심시간</strong>
                            ${currentHospital.breakStart && currentHospital.breakEnd ? `${currentHospital.breakStart} ~ ${currentHospital.breakEnd}` : '정보 없음'}
                        </div>
                        <div class="info-item">
                            <strong>예약 비용</strong>
                            ${currentHospital.reservationCost ? currentHospital.reservationCost + ' P' : '정보 없음'}
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>📅 예약하기</h3>
                    <div class="reservation-form">
                        <div class="form-grid">
                            <div class="form-group">
                                <label>예약 날짜</label>
                                <input type="date" id="reservationDate" required onchange="loadAvailableSlots(${hospitalId})">
                            </div>
                            <div class="form-group">
                                <label>예약 시간</label>
                                <select id="reservationTime" required onchange="onReservationTimeChange()">
                                    <option value="">시간을 선택해주세요</option>
                                </select>
                                <div id="availableSlotsLoading" class="loading" style="display:none; margin-top: 0.5rem;">
                                    <div class="spinner" style="width: 20px; height: 20px; margin: 0;"></div>
                                    <p style="font-size: 0.9rem; margin-top: 0.3rem;">예약 가능 시간 조회 중...</p>
                                </div>
                            </div>
                        </div>
                        <button class="btn btn-success" id="reservationSubmitBtn" onclick="showReservationConfirm(${hospitalId})" disabled>
                            예약하기
                        </button>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>⭐ 리뷰</h3>
                    <div id="reviewsList"></div>
                </div>
            </div>
        `;

        navigateTo('hospitalDetail');
        loadReviews(hospitalId);
    } catch (error) {
        console.error('Failed to load hospital detail:', error);
        showAlert('병원 정보를 불러올 수 없습니다', 'error');
    }
}

/**
 * 예약 가능한 시간대 조회
 */
async function loadAvailableSlots(hospitalId) {
    const dateInput = document.getElementById('reservationDate');
    const timeSelect = document.getElementById('reservationTime');
    const loadingDiv = document.getElementById('availableSlotsLoading');
    const submitBtn = document.getElementById('reservationSubmitBtn');

    const date = dateInput.value;

    if (!date) {
        timeSelect.innerHTML = '<option value="">시간을 선택해주세요</option>';
        submitBtn.disabled = true;
        return;
    }

    // 과거 날짜 체크
    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (selectedDate < today) {
        showAlert('과거 날짜는 선택할 수 없습니다', 'error');
        dateInput.value = '';
        timeSelect.innerHTML = '<option value="">시간을 선택해주세요</option>';
        submitBtn.disabled = true;
        return;
    }

    // 예약 가능한 시간대 (병원 영업 시간)
    const defaultTimes = ['09:00', '10:00', '11:00', '12:00', '14:00', '15:00', '16:00', '17:00'];

    let optionsHTML = '<option value="">시간을 선택해주세요</option>';
    defaultTimes.forEach(time => {
        optionsHTML += `<option value="${time}">📅 ${time}</option>`;
    });

    timeSelect.innerHTML = optionsHTML;
    submitBtn.disabled = true;
}

/**
 * 예약 시간 선택 시 submit 버튼 활성화
 */
function onReservationTimeChange() {
    const timeSelect = document.getElementById('reservationTime');
    const submitBtn = document.getElementById('reservationSubmitBtn');
    const dateInput = document.getElementById('reservationDate');

    if (dateInput.value && timeSelect.value) {
        submitBtn.disabled = false;
    } else {
        submitBtn.disabled = true;
    }
}

/**
 * 예약 확인 모달 표시
 */
function showReservationConfirm(hospitalId) {
    const date = document.getElementById('reservationDate').value;
    const time = document.getElementById('reservationTime').value;

    if (!date || !time) {
        showAlert('날짜와 시간을 선택해주세요', 'error');
        return;
    }

    const modalHTML = `
        <div class="modal active" id="reservationConfirmModal" onclick="if(event.target.id==='reservationConfirmModal'){closeReservationConfirmModal()}">
            <div class="modal-content">
                <div class="modal-header">
                    <h2>예약 확인</h2>
                    <span class="modal-close" onclick="closeReservationConfirmModal()">&times;</span>
                </div>
                <div style="padding: 1rem;">
                    <p><strong>병원:</strong> ${currentHospital.hospitalName}</p>
                    <p><strong>예약 날짜:</strong> ${formatDate(date + 'T' + time)}</p>
                    <p style="color: #666; margin-top: 1.5rem;">예약을 진행하시겠습니까?</p>
                    <div style="display: flex; gap: 1rem; margin-top: 1.5rem;">
                        <button class="btn btn-danger" style="flex: 1; margin: 0;" onclick="closeReservationConfirmModal()">취소</button>
                        <button class="btn btn-success" style="flex: 1; margin: 0;" onclick="confirmCreateReservation(${hospitalId})">확인</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

/**
 * 예약 확인 모달 닫기
 */
function closeReservationConfirmModal() {
    const modal = document.getElementById('reservationConfirmModal');
    if (modal) modal.remove();
}

/**
 * 예약 생성 확인 후 실제 생성
 */
function confirmCreateReservation(hospitalId) {
    closeReservationConfirmModal();
    createReservation(hospitalId);
}

// Reservation submission flag to prevent duplicate requests
let isSubmittingReservation = false;

async function createReservation(hospitalId) {
    if (!authToken) {
        showAlert('로그인이 필요합니다', 'info');
        navigateTo('login');
        return;
    }

    if (isSubmittingReservation) {
        showAlert('예약이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    const date = document.getElementById('reservationDate').value;
    const time = document.getElementById('reservationTime').value;

    if (!date || !time) {
        showAlert('날짜와 시간을 선택해주세요', 'error');
        return;
    }

    // Check if selected date is not in the past
    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (selectedDate < today) {
        showAlert('과거 날짜는 선택할 수 없습니다', 'error');
        return;
    }

    // Disable submit button
    const submitBtn = document.querySelector('button[onclick*="createReservation"]');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = '예약 중...';
    }

    isSubmittingReservation = true;

    try {
        // 백엔드 기대 포맷: yyyy-MM-dd'T'HH:mm (초 없음)
        const appointmentDateTime = `${date}T${time}`;

        await reservationsAPI.create({
            hospitalId: hospitalId,
            appointmentDate: appointmentDateTime
        });

        showAlert('예약이 완료되었습니다! 예약 내역에서 확인할 수 있습니다.', 'success');

        // Clear form and navigate to reservations
        if (date) document.getElementById('reservationDate').value = '';
        if (time) document.getElementById('reservationTime').value = '';

        // Load updated reservations list
        loadReservations();
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 409) {
                showAlert('해당 시간대는 이미 예약되었습니다', 'error');
            } else if (error.status === 400) {
                showAlert('예약 정보가 올바르지 않습니다', 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else {
                showAlert('예약 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('예약 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Reservation error:', error);
    } finally {
        isSubmittingReservation = false;
        // Re-enable submit button
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '예약하기';
        }
    }
}

// Reviews Functions
async function loadReviews(hospitalId) {
    try {
        const response = await reviewsAPI.getByHospital(hospitalId);
        const reviewsList = document.getElementById('reviewsList');

        // API 응답이 배열이 아닐 수 있으므로 처리
        let reviews = Array.isArray(response) ? response : (response?.content || []);

        if (!reviews || reviews.length === 0) {
            reviewsList.innerHTML = '<p>리뷰가 없습니다</p>';
            return;
        }

        reviewsList.innerHTML = reviews.map(review => `
            <div style="background: white; padding: 1rem; margin-bottom: 1rem; border-radius: 4px; border-left: 4px solid #667eea;">
                <strong>${review.content || '리뷰'}</strong>
                <p style="color: #666; margin-top: 0.5rem;">평점: ⭐ ${review.point || 0}</p>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load reviews:', error);
    }
}

// Reservations Functions
async function loadReservations() {
    const reservationsList = document.getElementById('reservationsList');
    reservationsList.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    try {
        const data = await reservationsAPI.getMyReservations();
        reservations = Array.isArray(data) ? data : (data.content || []);

        renderReservations(reservations);
    } catch (error) {
        console.error('Failed to load reservations:', error);
        reservationsList.innerHTML = '<div class="empty-state"><p>예약 정보를 불러올 수 없습니다</p></div>';
    }
}

function renderReservations(reservationsList) {
    const container = document.getElementById('reservationsList');

    if (!reservationsList || reservationsList.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>예약 내역이 없습니다</p></div>';
        return;
    }

    container.innerHTML = `
        <div class="reservations-list">
            ${reservationsList.map(reservation => `
                <div class="reservation-card">
                    <h3>${reservation.hospitalName || '병원'}</h3>
                    <span class="status ${reservation.status?.toLowerCase() || 'pending'}">
                        ${getStatusLabel(reservation.status)}
                    </span>
                    <div class="reservation-info">
                        <p><strong>예약 ID:</strong> ${reservation.id}</p>
                        <p><strong>병원명:</strong> ${reservation.hospitalName || '정보 없음'}</p>
                        <p><strong>예약 날짜/시간:</strong> ${reservation.appointmentDate ? formatDate(reservation.appointmentDate) : '정보 없음'}</p>
                        <p><strong>상태:</strong> ${getStatusLabel(reservation.status)}</p>
                        <p><strong>사용 포인트:</strong> ${reservation.usedPoints || 0} P</p>
                        ${reservation.memo ? `<p><strong>메모:</strong> ${reservation.memo}</p>` : ''}
                    </div>
                    <div class="reservation-actions">
                        ${reservation.status === 'PENDING' ? `
                            <button class="btn btn-danger" onclick="cancelReservation(${reservation.id})">
                                예약 취소
                            </button>
                        ` : ''}
                        <button class="btn btn-secondary" onclick="openReservationDetail(${reservation.id})">
                            상세보기
                        </button>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

async function openReservationDetail(reservationId) {
    try {
        const data = await reservationsAPI.getById(reservationId);
        const reservation = data; // apiCall returns data
        const html = `
            <div class="modal active" id="reservationModal" onclick="if(event.target.id==='reservationModal'){closeReservationModal()}">
                <div class="modal-content">
                    <div class="modal-header">
                        <h2>예약 상세</h2>
                        <span class="modal-close" onclick="closeReservationModal()">&times;</span>
                    </div>
                    <div>
                        <p><strong>병원:</strong> ${reservation.hospitalName || ''}</p>
                        <p><strong>예약일시:</strong> ${formatDate(reservation.appointmentDate)}</p>
                        <p><strong>상태:</strong> ${getStatusLabel(reservation.status)}</p>
                        <p><strong>사용 포인트:</strong> ${reservation.usedPoints || 0} P</p>
                        ${reservation.memo ? `<p><strong>메모:</strong> ${reservation.memo}</p>` : ''}
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', html);
    } catch (e) {
        showAlert('예약 상세를 불러오지 못했습니다: ' + e.message, 'error');
    }
}

function closeReservationModal() {
    const modal = document.getElementById('reservationModal');
    if (modal) modal.remove();
}

// Flag to prevent duplicate cancellation requests
let isCancellingReservation = false;

async function cancelReservation(reservationId) {
    if (!confirm('예약을 취소하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        return;
    }

    if (isCancellingReservation) {
        showAlert('취소 작업이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    // Find and disable the cancel button
    const cancelBtn = document.querySelector(`button[onclick="cancelReservation(${reservationId})"]`);
    if (cancelBtn) {
        cancelBtn.disabled = true;
        cancelBtn.textContent = '취소 중...';
    }

    isCancellingReservation = true;

    try {
        await reservationsAPI.cancel(reservationId);
        showAlert('예약이 성공적으로 취소되었습니다', 'success');
        loadReservations();
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 404) {
                showAlert('예약을 찾을 수 없습니다', 'error');
            } else if (error.status === 400) {
                showAlert('이미 취소된 예약이거나 취소할 수 없는 상태입니다', 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else {
                showAlert('예약 취소 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('예약 취소 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Cancel reservation error:', error);
    } finally {
        isCancellingReservation = false;
        // Re-enable the cancel button
        if (cancelBtn) {
            cancelBtn.disabled = false;
            cancelBtn.textContent = '예약 취소';
        }
    }
}

// Points Functions
let pointsCurrentFilter = 'ALL'; // ALL, DEPOSIT, SPEND, WITHDRAW, REFUND
let pointsTransactionPage = 0;
let pointsTransactionPageSize = 10;
let pointsTransactionTotal = 0;
let pointsTransactionTotalPages = 0;
let pointsTransactionCurrentBalance = 0;

async function loadPoints(page = 0) {
    const pointsContent = document.getElementById('pointsContent');

    // 스켈레톤 로딩 표시
    pointsContent.innerHTML = `
        <div class="points-container">
            <div class="points-balance-card">
                <div class="points-balance-header">
                    <h2>현재 포인트</h2>
                    <button class="btn btn-sm btn-secondary" disabled title="로딩 중">🔄</button>
                </div>
                <div class="skeleton-balance-amount" style="height: 60px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 2s infinite; border-radius: 8px; margin: 1rem 0;"></div>
            </div>
        </div>
    `;

    try {
        pointsTransactionPage = page;
        const balance = await pointsAPI.getBalance();
        const logs = await pointsAPI.getTransactionHistory(pointsTransactionPage, pointsTransactionPageSize);

        pointsTransactionCurrentBalance = balance.balance || 0;
        const pointsLog = Array.isArray(logs) ? logs : (logs.content || []);
        pointsTransactionTotal = Array.isArray(logs) ? pointsLog.length : (logs.totalElements || 0);
        pointsTransactionTotalPages = Array.isArray(logs) ? 1 : (logs.totalPages || 1);

        renderPointsPage(balance, pointsLog);
        setupPointsEventListeners();
    } catch (error) {
        console.error('Failed to load points:', error);
        pointsContent.innerHTML = `
            <div class="empty-state">
                <p>포인트 정보를 불러올 수 없습니다</p>
                <p style="color: #666; font-size: 0.9rem; margin: 1rem 0;">
                    ${error.message || '네트워크 오류가 발생했습니다'}
                </p>
                <button class="btn btn-primary" style="width: auto; margin-top: 1rem;" onclick="loadPoints(0)">
                    다시 시도
                </button>
            </div>
        `;
    }
}

/**
 * 포인트 페이지 렌더링
 */
function renderPointsPage(balance, logs) {
    const pointsContent = document.getElementById('pointsContent');

    // 거래 내역 필터링
    const filteredLogs = pointsCurrentFilter === 'ALL'
        ? logs
        : logs.filter(log => log.type === pointsCurrentFilter);

    // 날짜별 그룹핑
    const groupedByDate = groupTransactionsByDate(filteredLogs);

    pointsContent.innerHTML = `
        <div class="points-container">
            <!-- 포인트 잔액 -->
            <div class="points-balance-card">
                <div class="points-balance-header">
                    <h2>현재 포인트</h2>
                    <button class="btn btn-sm btn-secondary" onclick="loadPoints()" title="새로고침">🔄</button>
                </div>
                <div class="points-amount-display">
                    <div class="points-amount" id="pointsAmountDisplay">0</div>
                    <div class="points-unit">P</div>
                </div>
                <div class="points-amount-label">보유 중인 포인트</div>
            </div>

            <!-- 충전/환급 버튼 -->
            <div class="points-actions">
                <button class="btn btn-primary btn-large" onclick="showDepositModal()">
                    <span class="btn-icon">⬆️</span> 포인트 충전
                </button>
                <button class="btn btn-secondary btn-large" onclick="showWithdrawModal()">
                    <span class="btn-icon">⬇️</span> 포인트 환급
                </button>
            </div>

            <!-- 거래 내역 필터 -->
            <div class="points-filter">
                <h3>거래 내역</h3>
                <div class="filter-buttons">
                    <button class="filter-btn ${pointsCurrentFilter === 'ALL' ? 'active' : ''}" onclick="filterTransactions('ALL')">
                        전체
                    </button>
                    <button class="filter-btn ${pointsCurrentFilter === 'DEPOSIT' ? 'active' : ''}" onclick="filterTransactions('DEPOSIT')">
                        입금
                    </button>
                    <button class="filter-btn ${pointsCurrentFilter === 'REFUND' ? 'active' : ''}" onclick="filterTransactions('REFUND')">
                        환불
                    </button>
                    <button class="filter-btn ${pointsCurrentFilter === 'SPEND' ? 'active' : ''}" onclick="filterTransactions('SPEND')">
                        사용
                    </button>
                    <button class="filter-btn ${pointsCurrentFilter === 'WITHDRAW' ? 'active' : ''}" onclick="filterTransactions('WITHDRAW')">
                        출금
                    </button>
                </div>
            </div>

            <!-- 거래 내역 리스트 -->
            <div class="points-log">
                ${filteredLogs.length > 0 ? `
                    <div class="transaction-list">
                        ${Object.entries(groupedByDate).map(([date, items]) => `
                            <div class="transaction-group">
                                <div class="group-date">${formatTransactionDate(date)}</div>
                                <div class="group-items">
                                    ${items.map(log => `
                                        <div class="log-item ${getLogAmountClass(log.type)}">
                                            <div class="log-info">
                                                <span class="log-type">${getLogTypeLabel(log.type)}</span>
                                                <span class="log-description">${getLogDescription(log.type)}</span>
                                            </div>
                                            <div class="log-amount">
                                                ${getLogAmountSign(log.type)} ${log.amount} P
                                            </div>
                                        </div>
                                    `).join('')}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : `
                    <div class="empty-state">
                        <p>거래 내역이 없습니다</p>
                        <small>아직 포인트 거래가 없네요</small>
                    </div>
                `}
            </div>
        </div>
    `;

    // 포인트 카운트 애니메이션
    animatePointsAmount(pointsTransactionCurrentBalance);
}

/**
 * 포인트 잔액 카운트업 애니메이션
 */
function animatePointsAmount(targetAmount) {
    const display = document.getElementById('pointsAmountDisplay');
    if (!display) return;

    const duration = 800; // 밀리초
    const startTime = Date.now();
    const currentAmount = parseInt(display.textContent) || 0;

    const animateFrame = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);

        const current = Math.floor(currentAmount + (targetAmount - currentAmount) * progress);
        display.textContent = current.toLocaleString('ko-KR');

        if (progress < 1) {
            requestAnimationFrame(animateFrame);
        } else {
            display.textContent = targetAmount.toLocaleString('ko-KR');
        }
    };

    animateFrame();
}

/**
 * 거래 내역을 날짜별로 그룹핑
 */
function groupTransactionsByDate(logs) {
    const grouped = {};

    logs.forEach(log => {
        // 날짜 추출 (ISO 형식: 2024-01-15)
        const date = log.createdDate ? log.createdDate.substring(0, 10) : new Date().toISOString().substring(0, 10);

        if (!grouped[date]) {
            grouped[date] = [];
        }
        grouped[date].push(log);
    });

    // 날짜별로 내림차순 정렬
    return Object.keys(grouped)
        .sort((a, b) => new Date(b) - new Date(a))
        .reduce((result, key) => {
            result[key] = grouped[key];
            return result;
        }, {});
}

/**
 * 거래 유형별 설명
 */
function getLogDescription(type) {
    const descriptions = {
        'DEPOSIT': '포인트 충전',
        'WITHDRAW': '포인트 출금',
        'SPEND': '예약 사용',
        'REFUND': '환불 처리'
    };
    return descriptions[type] || '거래';
}

/**
 * 거래 내역 필터링
 */
function filterTransactions(filterType) {
    pointsCurrentFilter = filterType;
    pointsTransactionPage = 0;
    loadPoints(0);
}

/**
 * 거래 내역 페이지 이동
 */
function goToPointsPage(page) {
    if (page < 0 || page >= pointsTransactionTotalPages) return;

    // Smooth scroll to transaction list before loading
    const transactionList = document.querySelector('.transaction-list');
    if (transactionList) {
        transactionList.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    loadPoints(page);
}

/**
 * 거래 날짜 포맷팅
 */
function formatTransactionDate(dateString) {
    const date = new Date(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const isToday = dateString === today.toISOString().substring(0, 10);
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const isYesterday = dateString === yesterday.toISOString().substring(0, 10);

    if (isToday) {
        return '오늘';
    } else if (isYesterday) {
        return '어제';
    } else {
        return new Date(dateString).toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            weekday: 'short'
        });
    }
}

/**
 * 포인트 충전 모달 표시
 */
function showDepositModal() {
    const modalHTML = `
        <div class="modal active" id="depositModal" onclick="if(event.target.id==='depositModal'){closeDepositModal()}">
            <div class="modal-content modal-lg">
                <div class="modal-header">
                    <h2>⬆️ 포인트 충전</h2>
                    <span class="modal-close" onclick="closeDepositModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="modal-notice">
                        <p>📌 <strong>안내:</strong> 포인트 충전 기능은 백엔드에서 구현 중입니다.</p>
                        <p style="margin-top: 0.5rem; font-size: 0.9rem; color: #666;">
                            실제 결제는 아니며, 개발 환경에서만 테스트됩니다.
                        </p>
                    </div>

                    <form id="depositForm" onsubmit="handleDepositSubmit(event)">
                        <div class="form-group">
                            <label>충전할 금액</label>
                            <div class="amount-input-group">
                                <input type="number" id="depositAmount" placeholder="충전 금액 입력 (최소 1,000P)"
                                       min="1000" step="1000" required>
                                <span class="amount-unit">P</span>
                            </div>
                            <small style="color: #666; display: block; margin-top: 0.5rem;">
                                최소 1,000P부터 충전 가능합니다
                            </small>
                        </div>

                        <div class="form-group">
                            <label>결제 방법</label>
                            <select id="depositMethod" required>
                                <option value="">선택해주세요</option>
                                <option value="card">신용카드</option>
                                <option value="bank">계좌이체</option>
                                <option value="mobile">휴대폰 결제</option>
                            </select>
                        </div>

                        <div class="modal-actions">
                            <button type="button" class="btn btn-secondary" onclick="closeDepositModal()">취소</button>
                            <button type="submit" class="btn btn-primary">충전하기</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

/**
 * 포인트 환급 모달 표시
 */
function showWithdrawModal() {
    const modalHTML = `
        <div class="modal active" id="withdrawModal" onclick="if(event.target.id==='withdrawModal'){closeWithdrawModal()}">
            <div class="modal-content modal-lg">
                <div class="modal-header">
                    <h2>⬇️ 포인트 환급</h2>
                    <span class="modal-close" onclick="closeWithdrawModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="modal-notice">
                        <p>📌 <strong>안내:</strong> 포인트 환급 기능은 백엔드에서 구현 중입니다.</p>
                        <p style="margin-top: 0.5rem; font-size: 0.9rem; color: #666;">
                            현재 보유 중인 포인트: <strong>${pointsTransactionCurrentBalance} P</strong>
                        </p>
                    </div>

                    <form id="withdrawForm" onsubmit="handleWithdrawSubmit(event)">
                        <div class="form-group">
                            <label>환급할 금액</label>
                            <div class="amount-input-group">
                                <input type="number" id="withdrawAmount" placeholder="환급 금액 입력"
                                       min="1000" step="1000" max="${pointsTransactionCurrentBalance}" required>
                                <span class="amount-unit">P</span>
                            </div>
                            <small style="color: #666; display: block; margin-top: 0.5rem;">
                                보유 포인트 범위 내에서 환급 가능합니다
                            </small>
                        </div>

                        <div class="form-group">
                            <label>수령 계좌</label>
                            <input type="text" id="withdrawAccount" placeholder="계좌번호 (형식: 123-456-789012)" required>
                            <small style="color: #666; display: block; margin-top: 0.5rem;">
                                환급금은 이 계좌로 입금됩니다
                            </small>
                        </div>

                        <div class="modal-actions">
                            <button type="button" class="btn btn-secondary" onclick="closeWithdrawModal()">취소</button>
                            <button type="submit" class="btn btn-primary">환급 신청</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

/**
 * 포인트 충전 폼 제출
 */
async function handleDepositSubmit(event) {
    event.preventDefault();

    const amount = parseInt(document.getElementById('depositAmount').value);
    const method = document.getElementById('depositMethod').value;

    if (!amount || amount < 1000) {
        showAlert('최소 1,000P 이상 충전해주세요', 'error');
        return;
    }

    showAlert(`${amount.toLocaleString('ko-KR')}P 충전이 백엔드에서 처리될 예정입니다`, 'info');
    closeDepositModal();
}

/**
 * 포인트 환급 폼 제출
 */
async function handleWithdrawSubmit(event) {
    event.preventDefault();

    const amount = parseInt(document.getElementById('withdrawAmount').value);
    const account = document.getElementById('withdrawAccount').value;

    if (!amount || amount < 1000) {
        showAlert('최소 1,000P 이상 환급해주세요', 'error');
        return;
    }

    if (amount > pointsTransactionCurrentBalance) {
        showAlert('보유 포인트를 초과할 수 없습니다', 'error');
        return;
    }

    if (!account || account.trim() === '') {
        showAlert('수령 계좌를 입력해주세요', 'error');
        return;
    }

    showAlert(`${amount.toLocaleString('ko-KR')}P 환급이 백엔드에서 처리될 예정입니다`, 'info');
    closeWithdrawModal();
}

/**
 * 포인트 충전 모달 닫기
 */
function closeDepositModal() {
    const modal = document.getElementById('depositModal');
    if (modal) modal.remove();
}

/**
 * 포인트 환급 모달 닫기
 */
function closeWithdrawModal() {
    const modal = document.getElementById('withdrawModal');
    if (modal) modal.remove();
}

/**
 * 포인트 페이지 이벤트 리스너 설정
 */
function setupPointsEventListeners() {
    // 추가 이벤트 설정이 필요한 경우 여기에 작성
}

// ===== Chatbot FAB (Floating Action Button) Functions =====

/**
 * 챗봇 창 토글
 */
function toggleChatbot() {
    const chatBody = document.getElementById('chatBody');
    const chatFab = document.getElementById('chatFab');

    if (!chatBody) return;

    const isOpen = chatBody.style.display !== 'none';

    if (isOpen) {
        // 닫기
        chatBody.style.display = 'none';
        if (chatFab) chatFab.style.opacity = '1';
    } else {
        // 열기
        chatBody.style.display = 'flex';
        if (chatFab) chatFab.style.opacity = '0';

        // 처음 열 때만 채팅 초기화
        if (!window.chatbotFabInitialized) {
            initChatbotFab();
            window.chatbotFabInitialized = true;
        }

        // 입력 포커스
        const chatInput = document.getElementById('chatInput');
        if (chatInput) chatInput.focus();
    }
}

/**
 * FAB 챗봇 초기화
 */
function initChatbotFab() {
    const chatMessages = document.getElementById('chatMessages');
    if (chatMessages && chatMessages.children.length === 0) {
        // 기존 연결이 없으면 새로 연결
        if (!window.fabChatConnected) {
            connectFabChatbot();
        }
    }
}

/**
 * FAB 채팅 메시지 전송
 */
function sendChatMessage() {
    const chatInput = document.getElementById('chatInput');
    if (!chatInput) return;

    const text = chatInput.value.trim();
    if (!text) return;

    // 사용자 메시지 표시
    appendFabChatMessage('user', text);
    chatInput.value = '';

    // 실제 백엔드 연결 (initChatPage와 동일한 로직)
    if (!window.fabChatStompClient || !window.fabChatConnected) {
        appendFabChatMessage('bot', '죄송합니다. 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.');
        return;
    }

    try {
        const payload = {
            sessionId: window.fabChatSessionId,
            content: text
        };

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        window.fabChatStompClient.send('/app/chat/send', headers, JSON.stringify(payload));
    } catch (error) {
        console.error('Error sending message:', error);
        appendFabChatMessage('bot', '메시지 전송 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
}

/**
 * FAB 채팅 메시지 추가
 */
function appendFabChatMessage(sender, text) {
    const container = document.getElementById('chatMessages');
    if (!container || !text) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = text;

    messageDiv.appendChild(contentDiv);
    container.appendChild(messageDiv);

    // 자동 스크롤
    container.scrollTop = container.scrollHeight;
}

/**
 * FAB WebSocket 연결 초기화
 */
function connectFabChatbot() {
    try {
        // 현재 호스트 기반 WebSocket URL 동적 생성
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const socket = new WebSocket(`${protocol}//${host}/ws/chat`);
        window.fabChatStompClient = Stomp.over(socket);
        window.fabChatStompClient.debug = null;

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        window.fabChatStompClient.connect(headers,
            () => {
                // 연결 성공
                window.fabChatConnected = true;

                // 개인 큐 구독
                window.fabChatStompClient.subscribe('/user/queue/reply', (message) => {
                    try {
                        const body = JSON.parse(message.body);
                        if (body && body.sessionId && !window.fabChatSessionId) {
                            window.fabChatSessionId = body.sessionId;
                        }

                        // 메시지 타입 확인 (상담사 vs AI)
                        const messageType = body?.type || 'AI';
                        let sender = 'bot'; // 기본값은 챗봇

                        if (messageType === 'CONSULTANT') {
                            sender = 'consultant'; // 상담사 메시지
                        } else if (messageType === 'SYSTEM') {
                            sender = 'system'; // 시스템 메시지
                        }

                        appendFabChatMessage(sender, body?.content || '');
                    } catch (e) {
                        console.error('Message parse error:', e);
                        appendFabChatMessage('bot', message.body || '');
                    }
                });

                // 인사말
                appendFabChatMessage('bot', '안녕하세요! 무엇을 도와드릴까요?');
            },
            (error) => {
                // 연결 실패
                window.fabChatConnected = false;
                console.error('STOMP connection error:', error);
                appendFabChatMessage('bot', '죄송합니다. 연결에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        );
    } catch (e) {
        window.fabChatConnected = false;
        console.error('Chat WebSocket error:', e);
        appendFabChatMessage('bot', '죄송합니다. 연결 중 오류가 발생했습니다.');
    }
}

// Utility Functions
function getStatusLabel(status) {
    const labels = {
        'PENDING': '대기중',
        'APPROVED': '승인됨',
        'REJECTED': '거절됨',
        'COMPLETED': '완료됨',
        'CANCELLED': '취소됨'
    };
    return labels[status] || status;
}

function getLogTypeLabel(type) {
    const labels = {
        'DEPOSIT': '충전',
        'WITHDRAW': '환급',
        'SPEND': '사용',
        'REFUND': '환불'
    };
    return labels[type] || type;
}

function getLogAmountClass(type) {
    if (type === 'DEPOSIT' || type === 'REFUND') {
        return 'positive';
    }
    return 'negative';
}

function getLogAmountSign(type) {
    if (type === 'DEPOSIT' || type === 'REFUND') {
        return '+';
    }
    return '-';
}

function formatDate(dateString) {
    const options = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    };
    return new Date(dateString).toLocaleString('ko-KR', options);
}

function showAlert(message, type = 'info') {
    // Simple alert for now - could be improved with a toast notification system
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;

    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
        mainContent.insertBefore(alertDiv, mainContent.firstChild);

        // Auto-remove alert after 5 seconds
        setTimeout(() => {
            alertDiv.remove();
        }, 5000);
    }
}

// Hospital Management Functions
async function loadHospitalReservations() {
    const content = document.getElementById('hospitalReservationsContent');
    content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    try {
        const data = await reservationsAPI.getHospitalReservations(currentUser.id);
        const reservationsList = Array.isArray(data) ? data : (data.content || []);

        if (!reservationsList || reservationsList.length === 0) {
            content.innerHTML = '<div class="empty-state"><p>예약이 없습니다</p></div>';
            return;
        }

        content.innerHTML = `
            <div class="reservations-list">
                ${reservationsList.map(reservation => `
                    <div class="reservation-card">
                        <h3>${reservation.patientName || '환자'}</h3>
                        <span class="status ${reservation.status?.toLowerCase() || 'pending'}">
                            ${getStatusLabel(reservation.status)}
                        </span>
                        <div class="reservation-info">
                            <p><strong>예약 날짜:</strong> ${formatDate(reservation.appointmentDate)}</p>
                            <p><strong>상태:</strong> ${getStatusLabel(reservation.status)}</p>
                            <p><strong>사용 포인트:</strong> ${reservation.usedPoints || 0} P</p>
                        </div>
                        <div class="reservation-actions">
                            ${reservation.status === 'PENDING' ? `
                                <button class="btn btn-success" onclick="approveReservation(${reservation.id})">
                                    승인
                                </button>
                                <button class="btn btn-danger" onclick="rejectReservation(${reservation.id})">
                                    거절
                                </button>
                            ` : ''}
                            ${reservation.status === 'APPROVED' ? `
                                <button class="btn btn-success" onclick="completeReservation(${reservation.id})">
                                    완료
                                </button>
                            ` : ''}
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } catch (error) {
        console.error('Failed to load hospital reservations:', error);
        content.innerHTML = '<div class="empty-state"><p>예약 정보를 불러올 수 없습니다</p></div>';
    }
}

// Flag to prevent duplicate reservation status update requests
let isUpdatingReservationStatus = false;

async function approveReservation(reservationId) {
    if (isUpdatingReservationStatus) {
        showAlert('작업이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    const approveBtn = document.querySelector(`button[onclick="approveReservation(${reservationId})"]`);
    if (approveBtn) {
        approveBtn.disabled = true;
        approveBtn.textContent = '승인 중...';
    }

    isUpdatingReservationStatus = true;

    try {
        await reservationsAPI.updateStatus(reservationId, 'APPROVED');
        showAlert('예약이 승인되었습니다', 'success');
        loadHospitalReservations();
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 404) {
                showAlert('예약을 찾을 수 없습니다', 'error');
            } else if (error.status === 400) {
                showAlert('이미 처리된 예약이거나 승인할 수 없는 상태입니다', 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else {
                showAlert('승인 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('승인 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Approve reservation error:', error);
    } finally {
        isUpdatingReservationStatus = false;
        if (approveBtn) {
            approveBtn.disabled = false;
            approveBtn.textContent = '승인';
        }
    }
}

async function rejectReservation(reservationId) {
    if (!confirm('예약을 거절하시겠습니까?')) {
        return;
    }

    if (isUpdatingReservationStatus) {
        showAlert('작업이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    const rejectBtn = document.querySelector(`button[onclick="rejectReservation(${reservationId})"]`);
    if (rejectBtn) {
        rejectBtn.disabled = true;
        rejectBtn.textContent = '거절 중...';
    }

    isUpdatingReservationStatus = true;

    try {
        await reservationsAPI.updateStatus(reservationId, 'REJECTED');
        showAlert('예약이 거절되었습니다', 'success');
        loadHospitalReservations();
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 404) {
                showAlert('예약을 찾을 수 없습니다', 'error');
            } else if (error.status === 400) {
                showAlert('이미 처리된 예약이거나 거절할 수 없는 상태입니다', 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else {
                showAlert('거절 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('거절 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Reject reservation error:', error);
    } finally {
        isUpdatingReservationStatus = false;
        if (rejectBtn) {
            rejectBtn.disabled = false;
            rejectBtn.textContent = '거절';
        }
    }
}

async function completeReservation(reservationId) {
    if (isUpdatingReservationStatus) {
        showAlert('작업이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    const completeBtn = document.querySelector(`button[onclick="completeReservation(${reservationId})"]`);
    if (completeBtn) {
        completeBtn.disabled = true;
        completeBtn.textContent = '완료 중...';
    }

    isUpdatingReservationStatus = true;

    try {
        await reservationsAPI.updateStatus(reservationId, 'COMPLETED');
        showAlert('예약이 완료되었습니다', 'success');
        loadHospitalReservations();
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 404) {
                showAlert('예약을 찾을 수 없습니다', 'error');
            } else if (error.status === 400) {
                showAlert('이미 처리된 예약이거나 완료할 수 없는 상태입니다', 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else {
                showAlert('완료 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('완료 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Complete reservation error:', error);
    } finally {
        isUpdatingReservationStatus = false;
        if (completeBtn) {
            completeBtn.disabled = false;
            completeBtn.textContent = '완료';
        }
    }
}

// Hospital Create (Admin)
let isCreatingHospital = false;

async function handleHospitalCreate(event) {
    event.preventDefault();

    const isAdmin = currentUser && (currentUser.userRole && String(currentUser.userRole).includes('ADMIN'));
    if (!authToken || !currentUser || !isAdmin) {
        showAlert('관리자만 병원 등록이 가능합니다', 'error');
        return;
    }

    // 기본 정보
    const hospitalName = document.getElementById('hcName').value.trim();
    const address = document.getElementById('hcAddress').value.trim();
    const doctorName = document.getElementById('hcDoctor').value.trim();
    const description = document.getElementById('hcDescription').value.trim();
    const isOpen = document.getElementById('hcOpen').checked;

    // 진료 시간
    const openTime = document.getElementById('hcOpenTime').value;
    const closeTime = document.getElementById('hcCloseTime').value;
    const breakStart = document.getElementById('hcBreakStart').value;
    const breakEnd = document.getElementById('hcBreakEnd').value;

    // 예약 비용
    const reservationCost = parseInt(document.getElementById('hcReservationCost').value) || 0;

    // Validate inputs
    if (!hospitalName) {
        showAlert('병원명은 필수입력 항목입니다', 'error');
        return;
    }

    if (!address) {
        showAlert('주소는 필수입력 항목입니다', 'error');
        return;
    }

    if (hospitalName.length < 2) {
        showAlert('병원명은 2자 이상이어야 합니다', 'error');
        return;
    }

    if (reservationCost < 0) {
        showAlert('예약 비용은 0 이상이어야 합니다', 'error');
        return;
    }

    if (isCreatingHospital) {
        showAlert('병원 등록이 진행 중입니다. 잠시만 기다려주세요.', 'info');
        return;
    }

    // Disable submit button
    const form = document.querySelector('form[onsubmit*="handleHospitalCreate"]');
    const submitBtn = form ? form.querySelector('button[type="submit"]') : null;
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = '등록 중...';
    }

    isCreatingHospital = true;

    try {
        // 백엔드 HospitalCreateRequest 필드에 맞춰 매핑
        const payload = {
            hospitalName: hospitalName,
            hospitalDescription: description || '',
            hospitalAddress: address,
            hospitalIsOpen: !!isOpen,
            doctorName: doctorName || '',
            reservationCost: reservationCost,
            openTime: openTime,
            closeTime: closeTime,
            breakStart: breakStart,
            breakEnd: breakEnd
        };

        await hospitalsAPI.create(payload);

        showAlert(`'${hospitalName}' 병원이 성공적으로 등록되었습니다!`, 'success');

        // Clear form
        if (form) form.reset();

        // Navigate to hospitals page
        setTimeout(() => {
            navigateTo('hospitals');
        }, 1500);
    } catch (error) {
        // Handle different error types
        if (error instanceof APIError) {
            if (error.status === 400) {
                showAlert('입력한 병원 정보가 올바르지 않습니다: ' + error.message, 'error');
            } else if (error.status === 401) {
                showAlert('세션이 만료되었습니다. 다시 로그인해주세요', 'error');
                navigateTo('login');
            } else if (error.status === 409) {
                showAlert('이미 등록된 병원입니다', 'error');
            } else {
                showAlert('병원 등록 실패: ' + error.message, 'error');
            }
        } else {
            showAlert('병원 등록 중 오류가 발생했습니다: ' + error.message, 'error');
        }
        console.error('Hospital creation error:', error);
    } finally {
        isCreatingHospital = false;
        // Re-enable submit button
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '병원 등록';
        }
    }
}

console.log('App module loaded');

// ===== Chatbot Page Implementation =====
let chatbotStompClient = null;
let chatbotConnected = false;
let chatbotSessionId = null;
let chatbotObserverInitialized = false;
let isUserScrolling = false;
let hasNewMessages = false;
let isSendingChatMessage = false;

/**
 * 채팅 메시지 컨테이너를 하단으로 스크롤
 */
function scrollChatToBottom() {
    const container = document.getElementById('chat-messages');
    if (!container) return;

    requestAnimationFrame(() => {
        container.scrollTop = container.scrollHeight;
    });
}

/**
 * 채팅 페이지 진입 시 WebSocket 연결 초기화
 */
function initChatPage() {
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const charCount = document.getElementById('char-count');

    if (!messageInput || !sendBtn) return;

    // 기존 연결이 있으면 재사용
    if (!chatbotConnected) {
        connectChatbot();
    }

    // 메시지 입력 시 전송 버튼 활성화/비활성화
    messageInput.addEventListener('input', (e) => {
        const text = e.target.value.trim();
        sendBtn.disabled = text.length === 0;

        // 문자 개수 표시
        charCount.textContent = `${e.target.value.length}/2000`;
    });

    // Enter 키로 전송 (Shift+Enter는 줄바꿈)
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendChatbotMessage();
        }
    });

    // 전송 버튼 클릭
    sendBtn.addEventListener('click', sendChatbotMessage);

    // 스크롤 감지: 사용자가 스크롤할 때
    const chatMessages = document.getElementById('chat-messages');
    if (chatMessages) {
        chatMessages.addEventListener('scroll', () => {
            const isAtBottom = chatMessages.scrollHeight - chatMessages.clientHeight <= chatMessages.scrollTop + 50;
            isUserScrolling = !isAtBottom;
            hasNewMessages = false;
        });
    }

    // MutationObserver로 새 메시지 추가 시 자동 스크롤
    initChatObserver();

    // 포커스
    messageInput.focus();
}

/**
 * WebSocket 연결 초기화
 */
function connectChatbot() {
    const connectionDot = document.getElementById('connection-dot');
    const connectionText = document.getElementById('connection-text');

    try {
        // 현재 호스트 기반 WebSocket URL 동적 생성
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const socket = new WebSocket(`${protocol}//${host}/ws/chat`);
        chatbotStompClient = Stomp.over(socket);
        chatbotStompClient.debug = function(msg) {
            console.log('STOMP DEBUG:', msg);
        }; // 디버그 로그 활성화

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
            console.log('WebSocket 연결 시도 - Token 포함:', authToken.substring(0, 20) + '...');
        } else {
            console.warn('WebSocket 연결 시도 - Token 없음!');
        }

        chatbotStompClient.connect(headers,
            () => {
                // 연결 성공
                chatbotConnected = true;
                if (connectionDot) {
                    connectionDot.classList.remove('offline');
                    connectionDot.classList.add('online');
                }
                if (connectionText) {
                    connectionText.textContent = '연결됨';
                }

                // 개인 큐 구독
                chatbotStompClient.subscribe('/user/queue/reply', (message) => {
                    try {
                        const body = JSON.parse(message.body);
                        if (body && body.sessionId && !chatbotSessionId) {
                            chatbotSessionId = body.sessionId;
                        }

                        // 상담원 연결 처리
                        if (body?.actionType === 'TRANSFER_TO_CONSULTANT') {
                            handleConsultantTransfer(body);
                        } else if (body?.actionType === 'SESSION_CLOSED') {
                            handleSessionClosed(body);
                        } else {
                            // 메시지 타입 확인 (상담사 vs AI)
                            const messageType = body?.type || 'AI';
                            let sender = 'bot'; // 기본값은 챗봇

                            if (messageType === 'CONSULTANT') {
                                sender = 'consultant'; // 상담사 메시지
                            } else if (messageType === 'SYSTEM') {
                                sender = 'system'; // 시스템 메시지
                            }

                            appendChatMessage(sender, body?.content || '');
                        }

                        // 사용자가 스크롤 중이면 새 메시지 표시, 아니면 자동 스크롤
                        if (isUserScrolling) {
                            hasNewMessages = true;
                        } else {
                            scrollChatToBottom();
                        }
                    } catch (e) {
                        console.error('Message parse error:', e);
                        appendChatMessage('bot', message.body || '');
                    }
                });

                // 접속 인사
                appendChatMessage('bot', '안녕하세요! 무엇을 도와드릴까요?');
                scrollChatToBottom();
            },
            (error) => {
                // 연결 실패
                chatbotConnected = false;
                if (connectionDot) {
                    connectionDot.classList.remove('online');
                    connectionDot.classList.add('offline');
                }
                if (connectionText) {
                    connectionText.textContent = '연결 실패';
                }
                console.error('STOMP connection error:', error);
                appendChatMessage('bot', '죄송합니다. 연결에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        );
    } catch (e) {
        chatbotConnected = false;
        console.error('Chat WebSocket error:', e);
        appendChatMessage('bot', '죄송합니다. 연결 중 오류가 발생했습니다.');
    }
}

/**
 * 채팅 메시지 추가
 */
function appendChatMessage(sender, text) {
    const container = document.getElementById('chat-messages');
    if (!container || !text) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = text;

    messageDiv.appendChild(contentDiv);
    container.appendChild(messageDiv);

    // 애니메이션 트리거
    requestAnimationFrame(() => {
        messageDiv.style.animation = 'none';
        requestAnimationFrame(() => {
            messageDiv.style.animation = '';
        });
    });
}

/**
 * 채팅 메시지 전송
 */
function sendChatbotMessage() {
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const typingIndicator = document.getElementById('typing-indicator');

    if (!messageInput) return;

    const text = messageInput.value.trim();
    if (!text) return;

    // 중복 전송 방지
    if (isSendingChatMessage) return;
    isSendingChatMessage = true;
    sendBtn.disabled = true;

    try {
        // 사용자 메시지 표시
        appendChatMessage('user', text);
        messageInput.value = '';
        document.getElementById('char-count').textContent = '0/2000';

        if (!chatbotStompClient || !chatbotConnected) {
            appendChatMessage('bot', '죄송합니다. 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.');
            isSendingChatMessage = false;
            sendBtn.disabled = false;
            return;
        }

        // 입력 중 표시
        if (typingIndicator) {
            typingIndicator.style.display = 'flex';
        }

        scrollChatToBottom();

        // 메시지 전송
        const payload = {
            sessionId: chatbotSessionId,
            content: text
        };

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        chatbotStompClient.send('/app/chat/send', headers, JSON.stringify(payload));

        // 타임아웃: 10초 후에도 응답이 없으면 입력 중 표시 제거
        setTimeout(() => {
            if (typingIndicator && typingIndicator.style.display !== 'none') {
                typingIndicator.style.display = 'none';
                isSendingChatMessage = false;
                sendBtn.disabled = text === '';
            }
        }, 10000);

    } catch (error) {
        console.error('Error sending message:', error);
        if (typingIndicator) {
            typingIndicator.style.display = 'none';
        }
        appendChatMessage('bot', '메시지 전송 중 오류가 발생했습니다. 다시 시도해주세요.');
        isSendingChatMessage = false;
        sendBtn.disabled = false;
    }
}

/**
 * 메시지 DOM 변경 감지하여 자동 스크롤
 */
function initChatObserver() {
    if (chatbotObserverInitialized) return;

    const target = document.getElementById('chat-messages');
    if (!target) return;

    try {
        const observer = new MutationObserver(() => {
            // 사용자가 스크롤 중이 아니면 하단으로 자동 스크롤
            if (!isUserScrolling) {
                scrollChatToBottom();
            }
        });

        observer.observe(target, { childList: true, subtree: false });
        chatbotObserverInitialized = true;
    } catch (e) {
        console.error('MutationObserver init error:', e);
    }
}

/**
 * 채팅 페이지 정리 (페이지 이동 시)
 */
function cleanupChatPage() {
    const messageInput = document.getElementById('message-input');
    if (messageInput) {
        messageInput.removeEventListener('input', null);
        messageInput.removeEventListener('keydown', null);
    }

    const sendBtn = document.getElementById('send-btn');
    if (sendBtn) {
        sendBtn.removeEventListener('click', null);
    }

    const chatMessages = document.getElementById('chat-messages');
    if (chatMessages) {
        chatMessages.removeEventListener('scroll', null);
    }
}

// ===== Form Validation Functions =====

/**
 * 비밀번호 입력 필드의 타입을 토글 (password ↔ text)
 */
function togglePasswordVisibility(fieldId) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    if (field.type === 'password') {
        field.type = 'text';
    } else {
        field.type = 'password';
    }
}

/**
 * 이메일 형식 검증 (실시간)
 */
function validateEmail(fieldId) {
    const field = document.getElementById(fieldId);
    const errorId = fieldId + 'Error';
    const errorField = document.getElementById(errorId);
    const formGroup = field.closest('.form-group');

    const email = field.value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!email) {
        errorField.textContent = '';
        formGroup.classList.remove('error', 'success');
        return true;
    }

    if (!emailRegex.test(email)) {
        errorField.textContent = '올바른 이메일 형식을 입력해주세요';
        formGroup.classList.remove('success');
        formGroup.classList.add('error');
        return false;
    }

    errorField.textContent = '';
    formGroup.classList.remove('error');
    formGroup.classList.add('success');
    return true;
}

/**
 * 사용자명 검증 (실시간)
 */
function validateUsername(fieldId) {
    const field = document.getElementById(fieldId);
    const errorId = fieldId + 'Error';
    const errorField = document.getElementById(errorId);
    const formGroup = field.closest('.form-group');

    const username = field.value.trim();

    if (!username) {
        errorField.textContent = '';
        formGroup.classList.remove('error', 'success');
        return true;
    }

    if (username.length < 2) {
        errorField.textContent = '사용자명은 2자 이상이어야 합니다';
        formGroup.classList.remove('success');
        formGroup.classList.add('error');
        return false;
    }

    if (username.length > 20) {
        errorField.textContent = '사용자명은 20자 이하여야 합니다';
        formGroup.classList.remove('success');
        formGroup.classList.add('error');
        return false;
    }

    errorField.textContent = '';
    formGroup.classList.remove('error');
    formGroup.classList.add('success');
    return true;
}

/**
 * 비밀번호 검증 (실시간)
 */
function validatePassword(fieldId) {
    const field = document.getElementById(fieldId);
    const errorId = fieldId + 'Error';
    const errorField = document.getElementById(errorId);
    const formGroup = field.closest('.form-group');

    const password = field.value;

    if (!password) {
        errorField.textContent = '';
        formGroup.classList.remove('error', 'success');
        return true;
    }

    if (password.length < 8) {
        errorField.textContent = '비밀번호는 최소 8자 이상이어야 합니다';
        formGroup.classList.remove('success');
        formGroup.classList.add('error');
        return false;
    }

    // 비밀번호 강도 체크 (영문, 숫자 조합)
    const hasNumber = /[0-9]/.test(password);
    const hasLetter = /[a-zA-Z]/.test(password);

    if (!hasNumber || !hasLetter) {
        errorField.textContent = '비밀번호는 영문과 숫자를 포함해야 합니다';
        formGroup.classList.remove('success');
        formGroup.classList.add('error');
        return false;
    }

    errorField.textContent = '';
    formGroup.classList.remove('error');
    formGroup.classList.add('success');
    return true;
}

/**
 * 폼의 모든 입력 필드에서 Enter 키 지원 추가
 */
function addFormEnterKeySupport() {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        const inputs = form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('keydown', (event) => {
                // Enter 키일 때
                if (event.key === 'Enter') {
                    // textarea에서는 기본 동작 유지 (줄바꿈)
                    if (input.tagName === 'TEXTAREA') {
                        return;
                    }

                    event.preventDefault();

                    // 폼의 submit 버튼 찾기
                    const submitBtn = form.querySelector('button[type="submit"]');
                    if (submitBtn) {
                        submitBtn.click();
                    }
                }
            });
        });
    });
}

// ===== 상담원 연결 처리 함수 =====

/**
 * 상담원 전환 처리
 * - AI Rate Limit 초과 시 호출됨
 * - 즉시 연결 또는 대기열에 추가
 */
function handleConsultantTransfer(response) {
    console.log('상담원 전환 처리:', response);

    const typingIndicator = document.getElementById('typing-indicator');
    if (typingIndicator) {
        typingIndicator.style.display = 'none';
    }

    // 응답 메시지 표시
    if (response.waitingPosition === 0) {
        // 즉시 연결됨
        appendChatMessage('system', '🎧 상담원을 연결하고 있습니다...');
        appendChatMessage('bot', response.content || '상담원이 곧 응답할 예정입니다.');
    } else {
        // 대기열에 추가됨
        appendChatMessage('system', `📊 현재 대기 순번: ${response.waitingPosition}번`);
        appendChatMessage('bot', response.content || '상담원과 연결되기 전까지 잠시만 기다려주세요.');
    }

    scrollChatToBottom();

    // 입력창 비활성화 (상담원 연결 후 활성화)
    const sendBtn = document.getElementById('send-btn');
    if (sendBtn) {
        sendBtn.disabled = true;
    }

    isSendingChatMessage = false;
}

/**
 * 세션 종료 처리
 */
function handleSessionClosed(response) {
    console.log('세션 종료:', response);

    const typingIndicator = document.getElementById('typing-indicator');
    if (typingIndicator) {
        typingIndicator.style.display = 'none';
    }

    appendChatMessage('system', '👋 ' + (response.content || '상담이 종료되었습니다. 이용해주셔서 감사합니다.'));

    // 입력창 비활성화
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    if (messageInput) {
        messageInput.disabled = true;
    }
    if (sendBtn) {
        sendBtn.disabled = true;
    }

    scrollChatToBottom();
    isSendingChatMessage = false;
}

// ===== 상담원 대시보드 구현 =====

let consultantStompClient = null;
let consultantConnected = false;
let currentSessionId = null;
let waitingSessions = [];
let activeSessions = [];
let consultantSessionsIntervalId = null;  // setInterval ID 저장

/**
 * 상담원 대시보드 초기화
 */
function initConsultantDashboard() {
    connectConsultantWebSocket();
    setupConsultantUI();
    loadConsultantSessions();
}

/**
 * 상담원 대시보드 정리 (페이지 벗어날 때 호출)
 */
function cleanupConsultantDashboard() {
    // setInterval 정리
    if (consultantSessionsIntervalId) {
        clearInterval(consultantSessionsIntervalId);
        consultantSessionsIntervalId = null;
    }

    // WebSocket 연결 종료
    if (consultantStompClient && consultantConnected) {
        try {
            consultantStompClient.disconnect(() => {
                console.log('상담원 WebSocket 연결 종료');
            });
        } catch (e) {
            console.error('WebSocket 종료 중 오류:', e);
        }
    }

    // 상태 초기화
    consultantConnected = false;
    currentSessionId = null;
    waitingSessions = [];
    activeSessions = [];
}

/**
 * 상담원 WebSocket 연결
 */
function connectConsultantWebSocket() {
    const connectionDot = document.getElementById('consultant-connection-dot');
    const connectionText = document.getElementById('consultant-connection-text');

    try {
        // 현재 호스트 기반 WebSocket URL 동적 생성
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const socket = new WebSocket(`${protocol}//${host}/ws/chat`);
        consultantStompClient = Stomp.over(socket);
        consultantStompClient.debug = null;

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        consultantStompClient.connect(headers,
            () => {
                // 연결 성공
                consultantConnected = true;
                if (connectionDot) {
                    connectionDot.classList.remove('offline');
                    connectionDot.classList.add('online');
                }
                if (connectionText) {
                    connectionText.textContent = '연결됨';
                }

                // 할당된 세션 구독
                consultantStompClient.subscribe('/user/queue/assigned', (message) => {
                    try {
                        const event = JSON.parse(message.body);
                        console.log('새 세션 할당:', event);
                        if (event.sessionId) {
                            currentSessionId = event.sessionId;
                            selectSession(event.sessionId);
                            loadSessionMessages(event.sessionId);
                        }
                    } catch (e) {
                        console.error('Session assignment parse error:', e);
                    }
                });

                // 상담원이 받을 메시지 구독 (사용자로부터의 메시지)
                consultantStompClient.subscribe('/user/queue/messages', (message) => {
                    try {
                        const msg = JSON.parse(message.body);
                        console.log('새 메시지 수신:', msg);

                        if (msg && msg.sessionId === currentSessionId) {
                            // 현재 선택된 세션의 메시지만 추가
                            const sender = msg.type === 'USER' ? 'user' : (msg.type === 'CONSULTANT' ? 'consultant' : 'system');
                            appendConsultantChatMessage(sender, msg.content);

                            // 자동 스크롤
                            const container = document.getElementById('consultant-chat-messages');
                            if (container) {
                                container.scrollTop = container.scrollHeight;
                            }
                        }
                    } catch (e) {
                        console.error('Message receive parse error:', e);
                    }
                });

                // 세션 종료 알림 구독
                consultantStompClient.subscribe('/user/queue/session-closed', (message) => {
                    try {
                        const event = JSON.parse(message.body);
                        console.log('세션 종료:', event);

                        if (event.sessionId === currentSessionId) {
                            appendConsultantChatMessage('system', '👋 ' + (event.content || '사용자가 세션을 종료했습니다.'));

                            // 입력 비활성화
                            const messageInput = document.getElementById('consultant-message-input');
                            const sendBtn = document.getElementById('consultant-send-btn');
                            if (messageInput) messageInput.disabled = true;
                            if (sendBtn) sendBtn.disabled = true;

                            // 3초 후 세션 초기화
                            setTimeout(() => {
                                currentSessionId = null;
                                const info = document.getElementById('activeSessionInfo');
                                if (info) info.innerHTML = '세션을 선택해주세요';
                                loadConsultantSessions();
                            }, 3000);
                        }
                    } catch (e) {
                        console.error('Session closed parse error:', e);
                    }
                });

                // 주기적으로 대기 세션 업데이트 (기존 타이머 정리 후 시작)
                if (consultantSessionsIntervalId) {
                    clearInterval(consultantSessionsIntervalId);
                }
                consultantSessionsIntervalId = setInterval(loadConsultantSessions, 5000);
            },
            (error) => {
                // 연결 실패
                consultantConnected = false;
                if (connectionDot) {
                    connectionDot.classList.remove('online');
                    connectionDot.classList.add('offline');
                }
                if (connectionText) {
                    connectionText.textContent = '연결 실패';
                }
                console.error('Consultant STOMP error:', error);
            }
        );
    } catch (e) {
        consultantConnected = false;
        console.error('Consultant WebSocket error:', e);
    }
}

/**
 * 상담원 UI 설정
 */
function setupConsultantUI() {
    const messageInput = document.getElementById('consultant-message-input');
    const sendBtn = document.getElementById('consultant-send-btn');
    const charCount = document.getElementById('consultant-char-count');

    if (!messageInput || !sendBtn) return;

    // 메시지 입력 시
    messageInput.addEventListener('input', (e) => {
        const text = e.target.value.trim();
        sendBtn.disabled = !currentSessionId || text.length === 0;
        charCount.textContent = `${e.target.value.length}/2000`;
    });

    // Enter 키로 전송
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendConsultantMessage();
        }
    });
}

/**
 * 상담원 대기/활성 세션 로드
 */
async function loadConsultantSessions() {
    try {
        // 1. 대기열 상태 조회
        const statusResponse = await fetch('/api/consultant/queue/status', {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (statusResponse.ok) {
            const status = await statusResponse.json();
            document.getElementById('waitingCount').textContent = status.waitingCount || 0;
            document.getElementById('activeCount').textContent = status.activeConsultants || 0;
        }

        // 2. 대기 중인 세션 목록 조회
        const sessionsResponse = await fetch('/api/consultant/waiting-sessions', {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (sessionsResponse.ok) {
            waitingSessions = await sessionsResponse.json();
        }
    } catch (error) {
        console.error('Failed to load sessions:', error);
        waitingSessions = [];
    }

    // 대기 세션 목록 업데이트
    renderWaitingSessions();
}

/**
 * 대기 세션 목록 렌더링
 */
function renderWaitingSessions() {
    const container = document.getElementById('waitingSessionsList');

    if (!waitingSessions || waitingSessions.length === 0) {
        container.innerHTML = '<p style="color: #999;">대기 중인 세션이 없습니다</p>';
        return;
    }

    container.innerHTML = waitingSessions.map((session) => `
        <div style="padding: 1rem; background: white; border-radius: 4px; margin-bottom: 0.5rem; cursor: pointer; border-left: 3px solid #667eea;"
             onclick="selectSession(${session.sessionId})">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h4 style="margin: 0 0 0.5rem 0;">${session.username || '사용자'}</h4>
                    <p style="margin: 0; color: #666; font-size: 0.9rem;">대기 순번: ${session.waitingPosition || '-'}</p>
                    <p style="margin: 0.25rem 0 0 0; color: #999; font-size: 0.85rem;">${new Date(session.startedAt).toLocaleTimeString('ko-KR')}</p>
                </div>
                <button class="btn btn-primary" onclick="pickSession(${session.sessionId})" style="margin: 0;">수락</button>
            </div>
        </div>
    `).join('');
}

/**
 * 세션 선택
 */
function selectSession(sessionId) {
    currentSessionId = sessionId;
    const info = document.getElementById('activeSessionInfo');

    if (info) {
        info.innerHTML = `세션 ID: ${sessionId} | 상담 중...`;
    }

    const messageInput = document.getElementById('consultant-message-input');
    const sendBtn = document.getElementById('consultant-send-btn');
    if (messageInput) {
        messageInput.disabled = false;
    }
    if (sendBtn) {
        sendBtn.disabled = false;
    }

    loadSessionMessages(sessionId);
}

/**
 * 세션 수락 (대기열에서 가져오기)
 * 서버 응답(/user/queue/assigned)을 받은 후 selectSession이 호출됨
 */
function pickSession(sessionId) {
    if (!consultantStompClient || !consultantConnected) {
        showAlert('연결이 끊어졌습니다', 'error');
        return;
    }

    try {
        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        // 메시지 전송 (sessionId를 반드시 포함해야 함)
        const payload = {
            sessionId: sessionId
        };
        consultantStompClient.send('/app/consultant/pick', headers, JSON.stringify(payload));
        console.log('세션 수락 요청 전송:', sessionId);
    } catch (error) {
        console.error('Error picking session:', error);
        showAlert('세션 수락 중 오류가 발생했습니다', 'error');
    }
}

/**
 * 세션 메시지 로드
 */
async function loadSessionMessages(sessionId) {
    const container = document.getElementById('consultant-chat-messages');
    container.innerHTML = '';

    try {
        const response = await fetch(`/api/chat/sessions/${sessionId}/messages`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (response.ok) {
            const messages = await response.json();
            messages.forEach(msg => {
                const sender = msg.type === 'USER' ? 'user' : (msg.type === 'CONSULTANT' ? 'consultant' : 'system');
                appendConsultantChatMessage(sender, msg.content);
            });

            // 스크롤
            container.scrollTop = container.scrollHeight;
        }
    } catch (error) {
        console.error('Failed to load messages:', error);
    }
}

/**
 * 상담원 채팅 메시지 추가
 */
function appendConsultantChatMessage(sender, text) {
    const container = document.getElementById('consultant-chat-messages');
    if (!container || !text) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = text;

    messageDiv.appendChild(contentDiv);
    container.appendChild(messageDiv);

    // 스크롤
    container.scrollTop = container.scrollHeight;
}

/**
 * 상담원 메시지 전송
 */
function sendConsultantMessage() {
    if (!currentSessionId) {
        showAlert('선택된 세션이 없습니다', 'error');
        return;
    }

    if (!consultantStompClient || !consultantConnected) {
        showAlert('연결이 끊어졌습니다', 'error');
        return;
    }

    const messageInput = document.getElementById('consultant-message-input');
    const text = messageInput.value.trim();

    if (!text) return;

    try {
        // 사용자 메시지 표시
        appendConsultantChatMessage('consultant', text);
        messageInput.value = '';
        document.getElementById('consultant-char-count').textContent = '0/2000';

        // 메시지 전송
        const payload = {
            sessionId: currentSessionId,
            userId: 0, // 사용자 ID는 백엔드에서 처리
            content: text
        };

        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }

        consultantStompClient.send('/app/consultant/send', headers, JSON.stringify(payload));
    } catch (error) {
        console.error('Error sending message:', error);
        showAlert('메시지 전송 중 오류가 발생했습니다', 'error');
    }
}
