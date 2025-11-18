// ===== Hospital Detail Page JavaScript =====

let currentHospital = null;
let hospitalId = null;
let currentReviewPage = 0;
let totalReviewPages = 0;
let selectedRating = 0;

// --- [병원 ID 추출] ---
function getHospitalIdFromUrl() {
    const pathParts = window.location.pathname.split('/');
    const id = pathParts[pathParts.length - 1];
    return id && !isNaN(id) ? parseInt(id) : null;
}

// --- [병원 상세 정보 로드] ---
async function loadHospitalDetail(id) {
    try {
        const url = `${window.API_URL}/api/hospitals/${id}`;
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': getToken() ? `Bearer ${getToken()}` : ''
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            if (apiResponse.success && apiResponse.data) {
                currentHospital = apiResponse.data;
                displayHospitalDetail(currentHospital);
                loadReviews(id, 0); // 리뷰 로드
            } else {
                showError('병원 정보를 불러올 수 없습니다.');
            }
        } else if (response.status === 404) {
            showError('존재하지 않는 병원입니다.');
        } else {
            showError('병원 정보를 불러오는데 실패했습니다.');
        }
    } catch (error) {
        console.error('Load hospital detail error:', error);
        showError('서버 연결에 실패했습니다.');
    }
}

// --- [병원 정보 표시] ---
function displayHospitalDetail(hospital) {
    const container = document.querySelector('.hospital-detail-container');

    const businessHours = hospital.openTime && hospital.closeTime
        ? `${formatTime(hospital.openTime)} - ${formatTime(hospital.closeTime)}`
        : '00:00 - 00:00';

    const breakTime = hospital.breakStart && hospital.breakEnd
        ? `${formatTime(hospital.breakStart)} - ${formatTime(hospital.breakEnd)}`
        : '점심시간 없음';

    const reservationCost = hospital.reservationCost
        ? `${hospital.reservationCost.toLocaleString()}P`
        : '0P';

    const statusClass = hospital.hospitalIsOpen ? 'open' : 'closed';
    const statusText = hospital.hospitalIsOpen ? '영업중' : '영업종료';

    container.innerHTML = `
        <!-- 병원 정보 카드 -->
        <div class="hospital-main-card">
            <!-- 왼쪽: 그라데이션 영역 -->
            <div class="hospital-image-area">
                <div class="hospital-placeholder-text">병원 이미지</div>
            </div>
            
            <!-- 오른쪽: 정보 영역 -->
            <div class="hospital-info-area">
                <div class="hospital-title-section">
                    <h1 class="hospital-name-text">${hospital.hospitalName || '병원 이름'}</h1>
                    <span class="status-badge-pill ${statusClass}">${statusText}</span>
                </div>
                
                <div class="hospital-info-grid">
                    <div class="info-item">
                        <span class="info-key">병원장</span>
                        <span class="info-val">${hospital.doctorName || '정보없음'}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-key">위치</span>
                        <span class="info-val">${hospital.hospitalAddress || '주소 정보없음'}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-key">영업 시간</span>
                        <span class="info-val">${businessHours}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-key">점심 시간</span>
                        <span class="info-val">${breakTime}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-key">예약금</span>
                        <span class="info-val-blue">${reservationCost}</span>
                    </div>
                </div>
                
                <div class="hospital-button-group">
                    <button class="btn-book" onclick="makeReservation()">예약하기</button>
                    <button class="btn-fav-outline" onclick="toggleFavorite()">
                        ${hospital.isFavorite ? '★' : '☆'} 즐겨찾기
                    </button>
                </div>
            </div>
        </div>
        
        <!-- 병원 소개 카드 -->
        <div class="hospital-desc-card">
            <h2 class="desc-title">${hospital.hospitalName || '병원 이름'}</h2>
            <p class="desc-content">${hospital.hospitalDescription || '병원 소개가 등록되지 않았습니다.'}</p>
        </div>
        
        <!-- 리뷰 섹션 -->
        <div class="reviews-card" id="reviewsSection">
            <div class="reviews-top">
                <div class="rating-display">
                    <span class="big-star">★</span>
                    <span class="big-score" id="avgRating">-</span>
                </div>
                <button class="btn-write" onclick="showReviewModal()">작성하기</button>
            </div>
            
            <div class="reviews-grid" id="reviewsList">
                <!-- 리뷰 카드들 -->
            </div>
            
            <div class="pagination-area" id="reviewPagination">
                <!-- 페이지네이션 -->
            </div>
        </div>
    `;
}

// --- [리뷰 목록 로드] ---
async function loadReviews(hospitalId, page = 0) {
    try {
        const url = `${window.API_URL}/api/hospitals/${hospitalId}/reviews?page=${page}&size=4`;
        const response = await fetch(url);

        if (response.ok) {
            const apiResponse = await response.json();
            if (apiResponse.success && apiResponse.data) {
                const reviewData = apiResponse.data;
                currentReviewPage = reviewData.number;
                totalReviewPages = reviewData.totalPages;

                displayReviews(reviewData.content);
                displayReviewPagination();

                // 평균 평점 계산
                if (reviewData.content.length > 0) {
                    const avgRating = (reviewData.content.reduce((sum, r) => sum + r.point, 0) / reviewData.content.length).toFixed(1);
                    document.getElementById('avgRating').textContent = avgRating;
                } else {
                    document.getElementById('avgRating').textContent = '-';
                }
            }
        }
    } catch (error) {
        console.error('Load reviews error:', error);
    }
}

// --- [리뷰 표시] ---
function displayReviews(reviews) {
    const reviewsList = document.getElementById('reviewsList');

    if (reviews.length === 0) {
        reviewsList.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: #999; padding: 40px;">아직 등록된 리뷰가 없습니다.</p>';
        return;
    }

    reviewsList.innerHTML = reviews.map(review => {
        const stars = '★'.repeat(review.point);
        return `
            <div class="review-box">
                <div class="review-top-row">
                    <div class="review-star-line">
                        <span class="review-star-icon">★</span>
                        <span class="review-point">${review.point}</span>
                    </div>
                    <div class="review-meta">
                        <div class="review-user">작성자 익명</div>
                        <div class="review-time">작성일 yyyy-mm-dd hh:mm</div>
                    </div>
                </div>
                <p class="review-text">${review.content}</p>
                <div class="review-bottom">리뷰 내용</div>
            </div>
        `;
    }).join('');
}

// --- [리뷰 페이지네이션] ---
function displayReviewPagination() {
    const pagination = document.getElementById('reviewPagination');

    if (totalReviewPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = `
        <button class="pagination-btn" onclick="loadReviews(${hospitalId}, ${currentReviewPage - 1})" 
                ${currentReviewPage === 0 ? 'disabled' : ''}>이전</button>
        <div class="pagination-numbers">
    `;

    const startPage = Math.max(0, currentReviewPage - 2);
    const endPage = Math.min(totalReviewPages - 1, currentReviewPage + 2);

    if (startPage > 0) {
        html += `<button class="pagination-number" onclick="loadReviews(${hospitalId}, 0)">1</button>`;
        if (startPage > 1) html += '<span class="pagination-dots">...</span>';
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="pagination-number ${i === currentReviewPage ? 'active' : ''}" 
                         onclick="loadReviews(${hospitalId}, ${i})">${i + 1}</button>`;
    }

    if (endPage < totalReviewPages - 1) {
        if (endPage < totalReviewPages - 2) html += '<span class="pagination-dots">...</span>';
        html += `<button class="pagination-number" onclick="loadReviews(${hospitalId}, ${totalReviewPages - 1})">${totalReviewPages}</button>`;
    }

    html += `
        </div>
        <button class="pagination-btn" onclick="loadReviews(${hospitalId}, ${currentReviewPage + 1})" 
                ${currentReviewPage >= totalReviewPages - 1 ? 'disabled' : ''}>다음</button>
    `;

    pagination.innerHTML = html;
}

// --- [시간 포맷] ---
function formatTime(timeString) {
    if (!timeString) return '';
    return timeString.substring(0, 5);
}

// --- [즐겨찾기 토글] ---
async function toggleFavorite() {
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    if (!currentHospital) return;

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/hospitals/${currentHospital.id}/favorites`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            if (apiResponse.success) {
                loadHospitalDetail(hospitalId);
                const message = apiResponse.data ?
                    '즐겨찾기에 추가되었습니다.' :
                    '즐겨찾기에서 제거되었습니다.';
                showMessage(message, 'success');
            }
        } else if (response.status === 401) {
            showMessage('로그인이 필요합니다.', 'error');
            setTimeout(() => showLoginModal(), 500);
        }
    } catch (error) {
        console.error('Toggle favorite error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [예약하기] ---
function makeReservation() {
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    if (!hospitalId) {
        showMessage('병원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    // 병원 ID를 쿼리 파라미터로 전달하며 예약 페이지로 이동
    window.location.href = `/reservation?hospitalId=${hospitalId}`;
}

// --- [리뷰 모달 열기] ---
function showReviewModal() {
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        setTimeout(() => showLoginModal(), 500);
        return;
    }

    selectedRating = 0;
    document.getElementById('reviewPoint').value = '';
    document.getElementById('reviewContent').value = '';

    // 별점 초기화
    document.querySelectorAll('.star-rating .star').forEach(star => {
        star.classList.remove('active');
    });

    document.getElementById('reviewModal').style.display = 'flex';
}

// --- [리뷰 모달 닫기] ---
function closeReviewModal() {
    document.getElementById('reviewModal').style.display = 'none';
}

// --- [리뷰 제출] ---
async function submitReview(event) {
    event.preventDefault();

    const point = parseInt(document.getElementById('reviewPoint').value);
    const content = document.getElementById('reviewContent').value.trim();

    if (!point || point < 1 || point > 5) {
        showMessage('별점을 선택해주세요.', 'error');
        return;
    }

    if (!content) {
        showMessage('리뷰 내용을 입력해주세요.', 'error');
        return;
    }

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/hospitals/${hospitalId}/reviews`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ point, content })
        });

        if (response.ok) {
            const apiResponse = await response.json();
            if (apiResponse.success) {
                showMessage('리뷰가 등록되었습니다.', 'success');
                closeReviewModal();
                loadReviews(hospitalId, 0);
            } else {
                showMessage(apiResponse.message || '리뷰 등록에 실패했습니다.', 'error');
            }
        } else if (response.status === 401) {
            showMessage('로그인이 필요합니다.', 'error');
            setTimeout(() => showLoginModal(), 500);
        } else {
            const error = await response.json();
            showMessage(error.message || '리뷰 등록에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Submit review error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

// --- [에러 표시] ---
function showError(message) {
    const container = document.querySelector('.hospital-detail-container');
    container.innerHTML = `
        <div class="error-container">
            <div class="error-message">${message}</div>
            <button class="back-btn" onclick="goBack()">목록으로 돌아가기</button>
        </div>
    `;
}

// --- [뒤로가기] ---
function goBack() {
    window.location.href = '/';
}

// --- [페이지 로드] ---
document.addEventListener('DOMContentLoaded', function() {
    hospitalId = getHospitalIdFromUrl();

    if (!hospitalId) {
        showError('잘못된 접근입니다.');
        return;
    }

    loadHospitalDetail(hospitalId);

    // 별점 클릭 이벤트
    document.addEventListener('click', function(e) {
        if (e.target.matches('.star-rating .star')) {
            const rating = parseInt(e.target.dataset.value);
            selectedRating = rating;
            document.getElementById('reviewPoint').value = rating;

            document.querySelectorAll('.star-rating .star').forEach((star, index) => {
                if (index < rating) {
                    star.classList.add('active');
                } else {
                    star.classList.remove('active');
                }
            });
        }
    });
});

console.log('✅ Hospital.js loaded');