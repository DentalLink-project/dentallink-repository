# DentalLink Frontend

DentalLink 프로젝트의 간단한 Vanilla HTML/CSS/JavaScript 프론트엔드입니다. 별도의 빌드 도구나 설치 없이 바로 실행할 수 있습니다.

## 프로젝트 구조

```
frontend/
├── index.html          # 메인 HTML 파일
├── styles.css          # 스타일시트
├── api.js              # API 클라이언트 및 통신
├── app.js              # 앱 로직 및 상태 관리
└── README.md           # 이 파일
```

## 시작하기

### 요구사항

- 백엔드 서버가 `http://localhost:9999`에서 실행 중 (로컬 개발 포트)
- 최신 버전의 웹 브라우저 (Chrome, Firefox, Safari 등)
- MySQL 데이터베이스 (localhost:3306, dentallink DB)
- Redis (localhost:6379)

### 실행 방법

#### 1. 백엔드 서버 시작

```bash
# 프로젝트 루트 디렉토리에서
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun -x test
```

**포트 설정:**
- 로컬 개발: **포트 9999** (application-local.yml)
- 프로덕션: **포트 8080** (application.yml)

#### 2. 프론트엔드 서버 실행

Python이 설치되어 있다면:
```bash
# Python 3
cd frontend
python -m http.server 8000

# 또는 Python 2
python -m SimpleHTTPServer 8000
```

Node.js가 설치되어 있다면:
```bash
cd frontend
npx http-server
```

#### 3. 브라우저에서 열기
```
http://localhost:8000
또는 IDE의 Live Server를 사용 (예: http://localhost:63342)
```

#### 4. 환경 변수 확인

`application-local.yml` 설정값:
- 서버 포트: **9999**
- MySQL: localhost:3306 (dentallink DB, root/1234)
- Redis: localhost:6379
- CORS 허용: http://localhost:8000, http://localhost:63342, http://localhost:3000

## 주요 기능

### 1. 인증 (Authentication)
- ✅ 로그인 / 회원가입
- ✅ JWT 토큰 기반 인증
- ✅ 로그아웃 및 세션 관리

### 2. 병원 관리 (Hospital Management)
- ✅ 병원 목록 조회 (한 페이지에 최대 100개)
- ✅ 병원 검색 기능 (실시간 500ms 디바운스)
- ✅ 병원 상세 정보 조회
- ✅ 진료 시간 확인 (월-금, 점심시간)
- ✅ 예약 비용 표시
- ✅ 병원 등록 (관리자: 기본정보, 일정, 예약비)

### 3. 예약 시스템 (Reservation System)
- ✅ 예약 생성 (날짜/시간 선택)
- ✅ 내 예약 조회 (상세한 정보 표시)
- ✅ 예약 상태 확인 (대기중, 승인, 완료, 취소 등)
- ✅ 예약 취소
- ✅ 병원 관리자 예약 관리 (승인/거절/완료)

### 4. 포인트 관리 (Point System)
- ✅ 포인트 잔액 조회 (카운트업 애니메이션)
- ✅ 거래 내역 확인 (날짜별 그룹핑)
- ✅ 거래 유형 필터링 (입금, 환불, 사용, 출금)
- ✅ 포인트 충전/환급 UI
- ✅ 포인트 새로고침 버튼

### 5. 채팅 (Chatbot)
- ✅ AI 상담사 채팅 (WebSocket/STOMP)
- ✅ 네비게이션 메뉴에 채팅 링크
- ✅ FAB (Floating Action Button) 채팅 창
- ✅ 연결 상태 표시
- ✅ 자동 스크롤 및 입력 중 표시

### 6. 리뷰 (Review System)
- ✅ 병원별 리뷰 조회
- ✅ 리뷰 생성 UI (백엔드 통합 필요)

## API 엔드포인트

프론트엔드는 다음의 백엔드 API와 통신합니다:

```
POST   /api/auth/login              # 로그인
POST   /api/auth/signup             # 회원가입
POST   /api/auth/logout             # 로그아웃

GET    /api/hospitals               # 병원 목록 (페이징)
GET    /api/hospitals/{id}          # 병원 상세정보
GET    /api/hospitals/{id}/schedule # 진료시간

POST   /api/reservations            # 예약 생성
GET    /api/reservations/my         # 내 예약 조회
DELETE /api/reservations/{id}       # 예약 취소
PATCH  /api/reservations/{id}/status # 상태 변경

GET    /api/point-accounts          # 포인트 잔액
GET    /api/point-logs              # 거래 내역

GET    /api/hospitals/{id}/reviews  # 리뷰 목록
POST   /api/hospitals/{id}/reviews  # 리뷰 작성
```

## 파일 설명

### index.html
- 모든 페이지의 HTML 구조 포함
- 네비게이션 바, 메인 컨텐츠, 푸터로 구성
- 다음의 페이지 포함:
  - 홈 (Home)
  - 로그인 (Login)
  - 회원가입 (Signup)
  - 병원 목록 (Hospitals)
  - 병원 상세 (Hospital Detail)
  - 내 예약 (Reservations)
  - 포인트 (Points)

### styles.css
- 전체 앱의 스타일 정의
- 반응형 디자인 (모바일 지원)
- 다크/라이트 테마 색상 팔레트
- 다양한 컴포넌트의 스타일:
  - 네비게이션 바
  - 카드 레이아웃
  - 폼 입력
  - 버튼
  - 알림 메시지

### api.js
- 백엔드 API와의 통신 담당
- 다음의 API 모듈 제공:
  - `authAPI` - 인증 관련
  - `hospitalsAPI` - 병원 관련
  - `reservationsAPI` - 예약 관련
  - `pointsAPI` - 포인트 관련
  - `reviewsAPI` - 리뷰 관련
  - `favoritesAPI` - 즐겨찾기 관련
  - `qnaAPI` - Q&A 관련
- JWT 토큰 자동 처리
- 에러 핸들링

### app.js
- 앱의 핵심 로직 구현
- 페이지 네비게이션
- 상태 관리 (currentUser, hospitals, reservations 등)
- UI 업데이트 함수
- 이벤트 핸들러
- 유틸리티 함수

## 최근 수정사항 (Latest Updates)

### 포트 구성 변경 (Port Configuration)
- **이전**: 포트 8080 → **현재**: 포트 9999 (로컬 개발)
- `application-local.yml`에서 `server.port: 9999` 설정
- 프론트엔드 API 호출 URL 변경: `http://localhost:9999/api`
- WebSocket 연결: `ws://localhost:9999/ws/chat`

### 병원 목록 (Hospital List)
- 페이지 사이즈 증가: `10` → `100` (한 페이지에서 더 많은 병원 표시)
- 병원 목록 최신화 완료 (모든 병원이 한 번에 로드됨)

### 병원 상세보기 (Hospital Detail)
- 진료 시간 정보 표시 추가: `openTime`, `closeTime` 필드
- 점심시간 표시: `breakStart`, `breakEnd` 필드
- 예약 비용 표시: `reservationCost` 필드

### 예약 시간 선택 (Reservation Time Selection)
- 예약 가능 시간 직접 제공: 09:00, 10:00, 11:00, 12:00, 14:00, 15:00, 16:00, 17:00
- 날짜 선택 후 자동으로 시간 옵션 활성화
- 시간 선택 후 "예약하기" 버튼 활성화

### 예약 조회 (Reservation View)
- 예약 정보 상세 표시 개선
- 예약 ID, 병원명, 예약 날짜/시간, 상태, 사용 포인트, 메모 표시

### 채팅 기능 (Chatbot)
- 네비게이션 메뉴에 "💬 채팅" 링크 추가
- FAB 채팅 버튼 기능 구현
- toggleChatbot(), sendChatMessage() 함수 구현
- WebSocket 포트 9999로 업데이트

### 병원 등록 (Hospital Registration)
- 필드셋을 이용한 폼 구조 개선
- 진료 시간 입력 필드 추가 (시작시간, 종료시간, 점심시간)
- 예약 비용 입력 필드 추가

## 구현된 기능

### 완성도 현황

| 기능 | 상태 | 설명 |
|------|------|------|
| 로그인/회원가입 | ✅ 완성 | JWT 기반 인증 |
| 병원 목록 | ✅ 완성 | 조회(100개), 검색, 페이징 |
| 병원 상세 | ✅ 완성 | 정보, 일정, 예약비, 예약 |
| 예약 생성 | ✅ 완성 | 날짜/시간 선택 |
| 예약 관리 | ✅ 완성 | 조회(상세), 취소 |
| 포인트 조회 | ✅ 완성 | 잔액, 거래내역, 필터링 |
| 채팅 | ✅ 완성 | 메뉴/FAB, WebSocket |
| 병원 등록 | ✅ 완성 | 기본정보, 일정, 예약비 |
| 로그아웃 | ✅ 완성 | 세션 정리 |

## 테스트 시나리오

### 1. 회원가입 및 로그인
```
1. "회원가입" 버튼 클릭
2. 이메일, 사용자명, 비밀번호 입력
3. 사용자 유형 선택 (일반 사용자/병원 관리자)
4. "회원가입" 버튼 클릭
5. "로그인" 클릭하여 이메일/비밀번호로 로그인
```

### 2. 병원 찾기 및 예약
```
1. "병원 찾기" 메뉴 클릭
2. 병원 검색 또는 리스트에서 병원 선택
3. "자세히 보기" 클릭
4. 예약 날짜/시간 선택
5. "예약하기" 버튼 클릭
```

### 3. 예약 관리
```
1. "내 예약" 메뉴 클릭
2. 예약 목록 확인
3. "예약 취소" 버튼으로 예약 취소 가능
```

### 4. 포인트 확인
```
1. "포인트" 메뉴 클릭
2. 현재 포인트 잔액 확인
3. 거래 내역 확인
```

## 브라우저 호환성

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 알려진 제한사항

1. **CORS**: 백엔드에서 CORS를 허용해야 함
2. **로컬 스토리지**: 토큰은 localStorage에 저장 (보안 주의)
3. **이미지**: 병원 이미지 미지원 (텍스트 기반)
4. **실시간**: WebSocket 미지원 (폴링만 가능)
5. **모바일**: 기본 반응형이지만 최적화 필요

## 향후 개선 사항

- [ ] 이미지 업로드 및 표시
- [ ] 더 정교한 캘린더 UI
- [ ] 실시간 알림 시스템
- [ ] 결제 시스템 통합
- [ ] Q&A 시스템 완성
- [ ] 즐겨찾기 기능
- [ ] 로그인 유지 (Remember Me)
- [ ] 다크 모드
- [ ] 다국어 지원

## 에러 해결

### 포트 8080 이미 사용 중 (Port 8080 already in use)
```bash
# 포트 9999를 사용하도록 application-local.yml에서 설정했습니다.
# 포트 충돌이 발생하면:
pkill -9 java  # 모든 Java 프로세스 종료
lsof -i :9999  # 포트 사용 여부 확인
```

### API 호출 실패
- 백엔드 서버가 포트 9999에서 실행 중인지 확인
- `SPRING_PROFILES_ACTIVE=local`으로 시작했는지 확인
- 프론트엔드에서 `http://localhost:9999/api`로 호출 확인
- 브라우저 콘솔(F12)에서 에러 메시지 확인

### CORS 에러
- 백엔드 `application-local.yml`의 CORS 설정 확인:
  - `http://localhost:8000`
  - `http://localhost:63342` (IDE Live Server)
  - `http://localhost:3000`

### WebSocket 연결 실패 (채팅 미작동)
- 서버가 `ws://localhost:9999/ws/chat`에서 수신 중인지 확인
- 브라우저 개발자 도구 > Network > WS 탭에서 WebSocket 연결 상태 확인

### 로그인 실패
- 이메일/비밀번호 확인
- 회원가입 여부 확인
- 백엔드 로그 확인 (`SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` 출력)
- 테스트 계정: `admin@example.com` / `passwordA123!`

## 개발 팁

### 콘솔 디버깅
브라우저 개발자 도구 (F12)에서:
```javascript
// 현재 사용자 정보 확인
console.log(currentUser);

// 로컬 토큰 확인
console.log(localStorage.getItem('authToken'));

// API 호출 테스트
await hospitalsAPI.getAll();
```

### 페이지 수동 이동
```javascript
navigateTo('hospitals');  // 병원 목록
navigateTo('reservations');  // 예약 목록
navigateTo('points');  // 포인트
```

## 라이선스

MIT License

## 문의

기술 문제나 버그 리포트는 GitHub Issues를 통해 제출해주세요.
