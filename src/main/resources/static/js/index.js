// ===== Index Page JavaScript =====

// 페이지네이션 상태
let currentPage = 0;
let totalPages = 0;
const pageSize = 10;

// --- [병원 목록 로드] ---
async function loadHospitals(page = 0) {
    try {
        // API는 1부터 시작, 내부적으로는 0부터 시작
        const apiPage = page + 1;
        const url = `${window.API_URL}/api/hospitals?page=${apiPage}&size=${pageSize}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();

            if (apiResponse.success && apiResponse.data) {
                const pageData = apiResponse.data;

                // 병원 목록 표시
                displayHospitals(pageData.content);

                // 페이지네이션 정보 업데이트
                currentPage = pageData.number;
                totalPages = pageData.totalPages;
                updatePagination();
            } else {
                displayHospitals([]);
            }
        } else {
            console.error('Failed to load hospitals');
            showMessage('병원 목록을 불러오는데 실패했습니다.', 'error');
            displayHospitals([]);
        }
    } catch (error) {
        console.error('Load hospitals error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
        displayHospitals([]);
    }
}

// --- [병원 목록 표시] ---
function displayHospitals(hospitals) {
    const grid = document.getElementById('hospitalGrid');

    if (!hospitals || hospitals.length === 0) {
        grid.innerHTML = '<div class="empty-state">검색 결과가 없습니다.</div>';
        return;
    }

    grid.innerHTML = hospitals.map(hospital => `
        <div class="hospital-card">
            <div class="hospital-card-header">
                <div class="hospital-card-title">${escapeHtml(hospital.hospitalName || '병원이름')}</div>
                <div class="hospital-favorite" onclick="toggleFavorite(${hospital.id}, ${hospital.isFavorite})" data-hospital-id="${hospital.id}">
                    ${hospital.isFavorite ? SVGIcons.starFilled : SVGIcons.starOutlined}
                </div>
            </div>
            
            <div class="hospital-card-info">
                <div class="hospital-status-badge ${hospital.hospitalIsOpen ? 'open' : 'closed'}">
                    ${hospital.hospitalIsOpen ? '영업중' : '영업종료'}
                </div>
                <div class="hospital-info-row">
                    <span class="hospital-info-label">병원장</span>
                    <span class="hospital-info-value">${escapeHtml(hospital.doctorName || '정보없음')}</span>
                </div>
            </div>
            
            <div class="hospital-card-footer">
                <button class="hospital-detail-btn" onclick="viewHospitalDetail(${hospital.id})">
                    자세히 보기
                </button>
            </div>
        </div>
    `).join('');
}

// --- [즐겨찾기 토글] ---
async function toggleFavorite(hospitalId, currentStatus) {
    // 로그인 확인
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/hospitals/${hospitalId}/favorites`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();

            if (apiResponse.success) {
                // response.data 값으로 즐겨찾기 상태 확인 (boolean)
                const newFavoriteStatus = apiResponse.data;

                // UI 업데이트
                updateFavoriteIcon(hospitalId, newFavoriteStatus);

                const message = newFavoriteStatus ?
                    '즐겨찾기에 추가되었습니다.' :
                    '즐겨찾기에서 제거되었습니다.';
                showMessage(message, 'success');
            } else {
                showMessage(apiResponse.message || '즐겨찾기 처리에 실패했습니다.', 'error');
            }
        } else if (response.status === 401) {
            showMessage('로그인이 필요합니다.', 'error');
            setTimeout(() => showLoginModal(), 500);
        } else {
            const error = await response.json();
            showMessage(error.message || '즐겨찾기 처리에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Toggle favorite error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [즐겨찾기 아이콘 업데이트] ---
function updateFavoriteIcon(hospitalId, isFavorite) {
    const favoriteElement = document.querySelector(`[data-hospital-id="${hospitalId}"]`);
    if (favoriteElement) {
        favoriteElement.innerHTML = isFavorite ? SVGIcons.starFilled : SVGIcons.starOutlined;
        favoriteElement.onclick = () => toggleFavorite(hospitalId, isFavorite);
    }
}

// --- [병원 상세 페이지 이동] ---
function viewHospitalDetail(hospitalId) {
    window.location.href = `/hospitals/${hospitalId}`;
}

// --- [페이지네이션 업데이트] ---
function updatePagination() {
    const pagination = document.getElementById('pagination');
    const paginationNumbers = document.getElementById('paginationNumbers');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (!pagination || !paginationNumbers) return;

    // 페이지가 1개 이하면 숨김
    if (totalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }

    pagination.style.display = 'flex';

    // 이전/다음 버튼 활성화 상태
    prevBtn.disabled = currentPage === 0;
    nextBtn.disabled = currentPage >= totalPages - 1;

    // 페이지 번호 생성
    paginationNumbers.innerHTML = '';

    const maxVisible = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

    // 끝에서 시작 위치 조정
    if (endPage - startPage < maxVisible - 1) {
        startPage = Math.max(0, endPage - maxVisible + 1);
    }

    // 첫 페이지
    if (startPage > 0) {
        const firstBtn = createPageButton(0, '1');
        paginationNumbers.appendChild(firstBtn);

        if (startPage > 1) {
            const dots = document.createElement('span');
            dots.className = 'pagination-dots';
            dots.textContent = '...';
            paginationNumbers.appendChild(dots);
        }
    }

    // 중간 페이지들
    for (let i = startPage; i <= endPage; i++) {
        const btn = createPageButton(i, String(i + 1));
        paginationNumbers.appendChild(btn);
    }

    // 마지막 페이지
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const dots = document.createElement('span');
            dots.className = 'pagination-dots';
            dots.textContent = '...';
            paginationNumbers.appendChild(dots);
        }

        const lastBtn = createPageButton(totalPages - 1, String(totalPages));
        paginationNumbers.appendChild(lastBtn);
    }
}

// --- [페이지 버튼 생성] ---
function createPageButton(pageNum, text) {
    const btn = document.createElement('button');
    btn.className = 'pagination-number';
    btn.textContent = text;

    if (pageNum === currentPage) {
        btn.classList.add('active');
    }

    btn.onclick = () => {
        loadHospitals(pageNum);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    return btn;
}

// --- [페이지 변경] ---
function changePage(delta) {
    const newPage = currentPage + delta;
    if (newPage >= 0 && newPage < totalPages) {
        loadHospitals(newPage);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

// --- [HTML 이스케이프] ---
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// --- [로그인 성공 후 처리] ---
function onLoginSuccess(user) {
    // 병원 목록 새로고침
    loadHospitals(currentPage);
}

// --- [로그아웃 후 처리] ---
function onLogout() {
    // 병원 목록 새로고침
    loadHospitals(0);
}

// --- [페이지 로드 시 실행] ---
document.addEventListener('DOMContentLoaded', function() {
    // 병원 목록 로드
    loadHospitals(0);
});

console.log('✅ index.js loaded');