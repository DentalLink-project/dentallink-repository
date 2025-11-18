// ===== My Reservations Page JavaScript =====

let currentPage = 0;
let currentStatus = 'ALL';
let currentSort = 'appointmentDate,desc';
let totalPages = 0;
let selectedReservation = null;

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

    // 예약 목록 로드
    loadReservations();

    // 필터 버튼 이벤트
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentStatus = this.dataset.status;
            currentPage = 0;
            loadReservations();
        });
    });

    // 정렬 select 이벤트
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', function() {
            currentSort = this.value;
            currentPage = 0;
            loadReservations();
        });
    }
});

// 예약 목록 로드
async function loadReservations() {
    try {
        const token = getToken();
        let url = `${window.API_URL}/api/reservations/my?page=${currentPage}&size=10&sort=${currentSort}`;
        
        // 상태 필터 추가
        if (currentStatus !== 'ALL') {
            url += `&status=${currentStatus}`;
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
            displayReservations(pageData.content);
            displayPagination(pageData);
        } else if (response.status === 401) {
            showMessage('로그인이 필요합니다.', 'error');
            setTimeout(() => showLoginModal(), 500);
        } else {
            const error = await response.json();
            showMessage(error.message || '예약 목록을 불러오는데 실패했습니다.', 'error');
            displayEmptyState();
        }
    } catch (error) {
        console.error('Load reservations error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
        displayEmptyState();
    }
}

// 예약 목록 표시
function displayReservations(reservations) {
    const listContainer = document.getElementById('reservationsList');
    
    if (!reservations || reservations.length === 0) {
        displayEmptyState();
        return;
    }

    listContainer.innerHTML = reservations.map(reservation => {
        const statusText = getStatusText(reservation.status);
        const statusClass = reservation.status.toLowerCase();
        const appointmentDate = formatDateTime(reservation.appointmentDate);
        
        return `
            <div class="reservation-card" onclick="showReservationDetail(${reservation.id})">
                <div class="reservation-info">
                    <span class="status-badge ${statusClass}">${statusText}</span>
                    <div class="reservation-details">
                        <h3 class="hospital-name">${reservation.hospitalName}</h3>
                        <p class="appointment-time">예약 일시  ${appointmentDate}</p>
                    </div>
                </div>
                <button class="btn-detail-view" onclick="event.stopPropagation(); showReservationDetail(${reservation.id})">
                    상세 보기
                </button>
            </div>
        `;
    }).join('');
}

// 빈 상태 표시
function displayEmptyState() {
    const listContainer = document.getElementById('reservationsList');
    listContainer.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">📋</div>
            <div class="empty-state-text">예약 내역이 없습니다.</div>
        </div>
    `;
}

// 페이지네이션 표시
function displayPagination(pageData) {
    const pagination = document.getElementById('pagination');
    
    if (pageData.totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = `
        <button class="pagination-btn" onclick="changePage(${currentPage - 1})" 
                ${currentPage === 0 ? 'disabled' : ''}>이전</button>
        <div class="pagination-numbers">
    `;

    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(pageData.totalPages - 1, currentPage + 2);

    if (startPage > 0) {
        html += `<button class="pagination-number" onclick="changePage(0)">1</button>`;
        if (startPage > 1) html += '<span class="pagination-dots">...</span>';
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="pagination-number ${i === currentPage ? 'active' : ''}" 
                         onclick="changePage(${i})">${i + 1}</button>`;
    }

    if (endPage < pageData.totalPages - 1) {
        if (endPage < pageData.totalPages - 2) html += '<span class="pagination-dots">...</span>';
        html += `<button class="pagination-number" onclick="changePage(${pageData.totalPages - 1})">${pageData.totalPages}</button>`;
    }

    html += `
        </div>
        <button class="pagination-btn" onclick="changePage(${currentPage + 1})" 
                ${currentPage >= pageData.totalPages - 1 ? 'disabled' : ''}>다음</button>
    `;

    pagination.innerHTML = html;
}

// 페이지 변경
function changePage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadReservations();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 예약 상세 보기
async function showReservationDetail(reservationId) {
    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/reservations/my`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            const reservation = apiResponse.data.content.find(r => r.id === reservationId);
            
            if (reservation) {
                selectedReservation = reservation;
                displayReservationDetailModal(reservation);
            }
        }
    } catch (error) {
        console.error('Load reservation detail error:', error);
        showMessage('예약 정보를 불러오는데 실패했습니다.', 'error');
    }
}

// 예약 상세 모달 표시
function displayReservationDetailModal(reservation) {
    const modal = document.getElementById('reservationDetailModal');
    const statusText = getStatusText(reservation.status);
    const statusClass = reservation.status.toLowerCase();
    
    // 상태 배지
    const statusBadge = document.getElementById('modalStatusBadge');
    statusBadge.textContent = statusText;
    statusBadge.className = `status-badge-modal status-badge ${statusClass}`;
    
    // 정보 채우기
    document.getElementById('modalHospitalName').textContent = reservation.hospitalName;
    document.getElementById('modalAppointmentDate').textContent = formatDateTime(reservation.appointmentDate);
    document.getElementById('modalCreatedAt').textContent = formatDateTime(reservation.createdAt);
    document.getElementById('modalUsedPoints').textContent = `${reservation.usedPoints}P`;
    document.getElementById('modalReservationId').textContent = reservation.id;
    
    // 버튼 이벤트
    document.getElementById('btnViewHospital').onclick = () => {
        window.location.href = `/hospitals/${reservation.hospitalId}`;
    };
    
    document.getElementById('btnCancelReservation').onclick = () => {
        cancelReservation(reservation.id);
    };
    
    modal.style.display = 'flex';
}

// 예약 상세 모달 닫기
function closeReservationDetailModal() {
    document.getElementById('reservationDetailModal').style.display = 'none';
    selectedReservation = null;
}

// 예약 취소
async function cancelReservation(reservationId) {
    if (!confirm('정말 예약을 취소하시겠습니까?')) {
        return;
    }

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/reservations/${reservationId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            showMessage('예약이 취소되었습니다.', 'success');
            closeReservationDetailModal();
            loadReservations();
        } else {
            const error = await response.json();
            showMessage(error.message || '예약 취소에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Cancel reservation error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// 상태 텍스트 변환
function getStatusText(status) {
    const statusMap = {
        'PENDING': '대기중',
        'APPROVED': '승인',
        'REJECTED': '거부',
        'CANCELLED': '취소',
        'COMPLETED': '완료'
    };
    return statusMap[status] || status;
}

// 날짜 포맷 (yyyy-mm-dd hh:mm)
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '-';
    
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

console.log('My-Reservations.js loaded');
