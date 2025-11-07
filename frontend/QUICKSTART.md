# DentalLink Frontend - 빠른 시작 가이드

## 5분 안에 시작하기

### 1단계: 백엔드 서버 실행
```bash
# 프로젝트 루트에서
./gradlew bootRun

# 또는
gradle bootRun

# 또는 IDE에서 Run 버튼 클릭
```

서버가 `http://localhost:8080`에서 실행되는지 확인하세요.

### 2단계: 프론트엔드 웹 서버 실행

**방법 1: Python 사용 (권장)**
```bash
cd frontend
python -m http.server 8000
```

**방법 2: Node.js/npm 사용**
```bash
cd frontend
npx http-server -p 8000
```

**방법 3: Node.js + Live Server**
```bash
npm install -g live-server
cd frontend
live-server --port=8000
```

**방법 4: VS Code Live Server 확장**
1. VS Code에서 Live Server 확장 설치
2. index.html 우클릭 → "Open with Live Server"

### 3단계: 브라우저에서 열기
```
http://localhost:8000
```

## 사용자 계정 생성 및 테스트

### 1. 회원가입
```
이메일: test@example.com
사용자명: TestUser
비밀번호: password123
사용자 유형: 일반 사용자
```

### 2. 로그인
위의 이메일과 비밀번호로 로그인

### 3. 주요 기능 테스트
- ✅ 병원 찾기 → 자세히 보기 → 예약하기
- ✅ 내 예약 → 예약 조회 및 취소
- ✅ 포인트 → 잔액 및 거래 내역 확인

## 파일 구조

```
dentallink/
├── src/
│   └── main/
│       ├── java/          # 백엔드 코드
│       └── resources/
│           ├── static/    # (현재 비어있음)
│           ├── templates/ # (현재 비어있음)
│           └── application.yml
│
└── frontend/              # ← 여기에 새로운 프론트엔드!
    ├── index.html
    ├── styles.css
    ├── api.js
    ├── app.js
    ├── README.md
    └── QUICKSTART.md
```

## 기능 체크리스트

### 인증 (Authentication)
- [x] 회원가입
- [x] 로그인
- [x] 로그아웃
- [x] JWT 토큰 관리

### 병원 (Hospitals)
- [x] 병원 목록 조회
- [x] 병원 검색
- [x] 병원 상세 정보
- [x] 진료 시간 표시

### 예약 (Reservations)
- [x] 예약 생성
- [x] 내 예약 조회
- [x] 예약 상태 표시 (대기/승인/완료/취소)
- [x] 예약 취소

### 포인트 (Points)
- [x] 포인트 잔액 조회
- [x] 거래 내역 확인
- [ ] 포인트 충전 (UI만 준비, 백엔드 연동 필요)
- [ ] 포인트 환급 (UI만 준비, 백엔드 연동 필요)

### 리뷰 (Reviews)
- [x] 리뷰 목록 조회
- [ ] 리뷰 작성 (UI 추가 필요)

## 문제 해결

### 에러: "Cannot GET /"
→ Python 웹 서버가 제대로 실행되었는지 확인

### 에러: "Cannot connect to API"
→ 백엔드 서버가 8080 포트에서 실행되는지 확인

### 에러: "CORS policy"
→ 백엔드의 CORS 설정 확인 (application.yml 참조)

### 로그인 실패
→ 회원가입한 계정이 있는지 확인
→ 백엔드 데이터베이스 연결 확인

## API 서버 CORS 설정

만약 CORS 에러가 발생하면, 백엔드의 보안 설정을 확인하세요:

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:8000,http://localhost:3000"
      allowed-methods: "GET,POST,PUT,PATCH,DELETE,OPTIONS"
      allowed-headers: "*"
      allow-credentials: true
```

## 다음 단계

### 추가 기능 구현
1. 병원 등록 (병원 관리자용)
2. 예약 승인/거절 (병원 관리자용)
3. 리뷰 작성/수정
4. Q&A 시스템
5. 즐겨찾기 기능

### 스타일 개선
1. 더 현대적인 UI/UX
2. 다크 모드
3. 애니메이션 추가
4. 이미지 배너

### 성능 최적화
1. 이미지 최적화
2. 코드 분할
3. 캐싱 전략

## 주요 함수 레퍼런스

```javascript
// 페이지 이동
navigateTo('hospitals');
navigateTo('reservations');
navigateTo('points');
navigateTo('login');

// API 호출
await hospitalsAPI.getAll();
await reservationsAPI.getMyReservations();
await pointsAPI.getBalance();

// 유틸리티
showAlert('메시지', 'success|error|info');
formatDate('2024-01-15T14:30:00');
```

## 콘솔에서 직접 테스트

브라우저 콘솔 (F12)에서:
```javascript
// 현재 상태 확인
console.log(currentUser);
console.log(authToken);

// API 직접 호출
await hospitalsAPI.getAll();

// 페이지 이동
navigateTo('hospitals');
```

---

**모든 준비가 되었으면 앱을 즐기세요! 🎉**

문제가 있으면 README.md를 참조하세요.
