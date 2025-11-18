// ===== Reservation Page JavaScript =====

// URL에서 병원 ID 추출
const urlParams = new URLSearchParams(window.location.search);
const hospitalId = urlParams.get('hospitalId');

// 병원 정보 저장
let hospitalData = null;

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

    // 병원 ID 체크
    if (!hospitalId) {
        showMessage('병원 정보가 없습니다.', 'error');
        setTimeout(() => {
            window.location.href = '/';
        }, 1500);
        return;
    }

    // 병원 정보 로드
    loadHospitalInfo();

    // 예약 버튼 이벤트
    const btnReserve = document.getElementById('btnReserve');
    if (btnReserve) {
        btnReserve.addEventListener('click', createReservation);
    }

    // 오늘 날짜 기본값 설정
    const today = new Date().toISOString().split('T')[0];
    const dateInput = document.getElementById('appointmentDate');
    if (dateInput) {
        dateInput.value = today;
        dateInput.min = today;
    }
});

// 병원 정보 로드
async function loadHospitalInfo() {
    try {
        const response = await fetch(`${window.API_URL}/api/hospitals/${hospitalId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (response.ok) {
            const apiResponse = await response.json();
            hospitalData = apiResponse.data || apiResponse;
            displayHospitalInfo(hospitalData);
        } else {
            showMessage('병원 정보를 불러오는데 실패했습니다.', 'error');
            setTimeout(() => {
                window.location.href = '/';
            }, 1500);
        }
    } catch (error) {
        console.error('Load hospital info error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
        setTimeout(() => {
            window.location.href = '/';
        }, 1500);
    }
}

// 병원 정보 표시
function displayHospitalInfo(hospital) {
    // 병원 이름
    const hospitalName = document.getElementById('hospitalName');
    if (hospitalName) {
        hospitalName.textContent = hospital.hospitalName || '병원 이름';
    }

    // 주소
    const hospitalAddress = document.getElementById('hospitalAddress');
    if (hospitalAddress) {
        hospitalAddress.textContent = hospital.hospitalAddress || '주소 정보 없음';
    }

    // 영업 시간
    const openTime = formatTime(hospital.openTime) || '09:00';
    const closeTime = formatTime(hospital.closeTime) || '18:00';
    const businessHours = document.getElementById('businessHours');
    if (businessHours) {
        businessHours.textContent = `${openTime} - ${closeTime}`;
    }

    // 점심 시간
    const breakStart = formatTime(hospital.breakStart) || '12:00';
    const breakEnd = formatTime(hospital.breakEnd) || '13:00';
    const breakTime = document.getElementById('breakTime');
    if (breakTime) {
        breakTime.textContent = `${breakStart} - ${breakEnd}`;
    }

    // 예약 포인트
    const reservationCost = hospital.reservationCost || 1000;
    const requiredPoints = document.getElementById('requiredPoints');
    if (requiredPoints) {
        requiredPoints.textContent = `${reservationCost}P`;
    }

    // 시간 옵션 동적 생성
    generateTimeOptions(openTime, closeTime, breakStart, breakEnd);
}

// 시간 포맷 (HH:MM:SS -> HH:MM)
function formatTime(timeString) {
    if (!timeString) return '';
    return timeString.substring(0, 5);
}

// 시간 옵션 동적 생성
function generateTimeOptions(openTime, closeTime, breakStart, breakEnd) {
    const timeSelect = document.getElementById('appointmentTime');
    if (!timeSelect) return;

    // 기존 옵션 제거 (첫 번째 "시간 선택" 제외)
    while (timeSelect.options.length > 1) {
        timeSelect.remove(1);
    }

    // 시간 문자열을 시간(숫자)으로 변환
    const openHour = parseInt(openTime.split(':')[0]);
    const closeHour = parseInt(closeTime.split(':')[0]);
    const breakStartHour = parseInt(breakStart.split(':')[0]);
    const breakEndHour = parseInt(breakEnd.split(':')[0]);

    // 영업 시간 내 정시 옵션 생성
    for (let hour = openHour; hour < closeHour; hour++) {
        // 점심시간 제외
        if (hour >= breakStartHour && hour < breakEndHour) {
            continue;
        }

        const timeString = String(hour).padStart(2, '0') + ':00';
        const option = document.createElement('option');
        option.value = timeString;
        option.textContent = timeString;
        timeSelect.appendChild(option);
    }
}

// 예약 생성
async function createReservation() {
    if (!getToken()) {
        showMessage('로그인이 필요합니다.', 'error');
        showLoginModal();
        return;
    }

    const dateInput = document.getElementById('appointmentDate');
    const timeInput = document.getElementById('appointmentTime');

    if (!dateInput || !dateInput.value) {
        showMessage('날짜를 선택해주세요.', 'error');
        return;
    }

    if (!timeInput || !timeInput.value) {
        showMessage('시간을 선택해주세요.', 'error');
        return;
    }

    // ISO 8601 형식으로 변환: yyyy-MM-ddTHH:mm
    const appointmentDateTime = `${dateInput.value}T${timeInput.value}`;

    // 요청 데이터
    const requestData = {
        hospitalId: parseInt(hospitalId),
        appointmentDate: appointmentDateTime
    };

    console.log('Creating reservation with data:', requestData);

    try {
        const token = getToken();
        const response = await fetch(`${window.API_URL}/api/reservations`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const apiResponse = await response.json();

        if (response.ok && apiResponse.success) {
            showMessage('예약이 완료되었습니다!', 'success');
            
            setTimeout(() => {
                window.location.href = '/';
            }, 1500);
        } else {
            showMessage(apiResponse.message || '예약에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('Create reservation error:', error);
        showMessage('서버 연결에 실패했습니다.', 'error');
    }
}

console.log('reservation.js loaded');
