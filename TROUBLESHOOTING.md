# 🔧 트러블슈팅(석호)

챗봇 기능 배포 과정에서 발생한 주요 기술적 문제와 해결 과정을 기록합니다.

## 목차
1. [Docker 네트워크 격리로 인한 Redis 연결 실패](#1-docker-네트워크-격리로-인한-redis-연결-실패)
2. [Spring ConfigurationProperties 바인딩 불일치](#2-spring-configurationproperties-바인딩-불일치)
3. [WebSocket 인증 우회 취약점](#3-websocket-인증-우회-취약점)
4. [CI/CD 환경 재현성 문제](#4-cicd-환경-재현성-문제)
5. [WebSocket 양방향 통신 응답 전송 실패](#5-websocket-양방향-통신-응답-전송-실패)
6. [WebSocket 메시지 라우팅 destination 불일치](#6-websocket-메시지-라우팅-destination-불일치)

---

## 1. Docker 네트워크 격리로 인한 Redis 연결 실패

### 문제
```
RedisConnectionException: Unable to connect to redis/<unresolved>:6379
```
배포 후 애플리케이션이 Redis에 연결할 수 없는 문제 발생

### 원인
Docker는 네트워크 단위로 컨테이너를 격리합니다. 서로 다른 네트워크에 있는 컨테이너는 통신이 불가능합니다.
```
기존 Redis 컨테이너: my-app-network
신규 배포된 App: bridge (기본 네트워크)
→ 네트워크가 다르면 hostname 해석 불가
```

### 해결
**임시:** 수동으로 네트워크 연결
```bash
docker network connect my-app-network app
```

**근본:** 배포 스크립트 수정
```bash
# scripts/deploy.sh
docker run -d \
  --name app \
  --network my-app-network \  # 네트워크 명시
  --restart=always \
  -p 8080:8080 \
  ${IMAGE}
```

### 배운 점
- Docker 컨테이너 간 통신은 같은 네트워크 내에서만 가능
- 개발 환경과 배포 환경의 인프라 구성이 일치해야 함
- Infrastructure as Code로 네트워크 설정까지 관리 필요

---

## 2. Spring ConfigurationProperties 바인딩 불일치

### 문제
```
Gemini API Error: API key not valid
```
AWS Parameter Store에 API 키가 저장되어 있으나, 애플리케이션에서 null로 인식되어 API 호출 실패

### 원인
외부 설정 파일의 구조와 `@ConfigurationProperties` 클래스 구조가 불일치

**application.yml (중첩 구조):**
```yaml
gemini:
  api:              # 중간 레벨 존재
    key: ${...}
    model: ...
```

**GeminiConfig (평면 구조):**
```java
@ConfigurationProperties(prefix = "gemini")
class GeminiConfig {
    private String apiKey;  // gemini.apiKey를 찾음
}
```
→ `gemini.api.key`와 `gemini.apiKey`가 매칭 안 됨

### 해결
설정 구조를 평면으로 통일
```yaml
gemini:
  api-key: ${gemini.api.key}      # kebab-case → camelCase 자동 매핑
  model-name: gemini-2.0-flash
  max-tokens: 2048
```

### 배운 점
- Spring Boot의 Relaxed Binding은 같은 레벨에서만 작동
- 외부 설정 구조와 Java 클래스 구조를 일치시켜야 함
- 중첩 구조 사용 시 `@ConfigurationProperties`도 중첩 클래스로 구성 필요

---

## 3. WebSocket 인증 우회 취약점

### 문제
REST API는 JWT 필터로 인증이 보호되지만, WebSocket은 토큰 없이도 연결이 가능한 보안 취약점 발견

### 원인
Interceptor에서 토큰이 없을 때 경고만 출력하고 연결을 허용
```java
if (token == null) {
    log.warn("토큰 없음");  // 경고만 하고 통과
}
// 다음 단계 계속 진행...
```

### 해결
토큰이 없으면 명시적으로 예외를 던져 연결 차단
```java
if (token == null) {
    log.error("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
    throw new RuntimeException("JWT 토큰이 필요합니다.");
}
```

### 배운 점
- 프로토콜별(REST/WebSocket) 보안 정책을 명확히 분리하고 각각 구현 필요
- 경고 로그와 예외 처리는 완전히 다른 의미
- 인증 실패는 반드시 명시적으로 차단해야 함

---

## 4. CI/CD 환경 재현성 문제

### 문제
GitHub Actions로 배포 후, 매번 수동으로 `docker network connect` 명령어를 실행해야 하는 번거로움

### 원인
배포 스크립트가 로컬 개발 환경의 인프라 구성(네트워크 설정)을 반영하지 못함
```bash
# 기존 deploy.sh
docker run -d --name app -p 8080:8080 ${IMAGE}
# → 기본 bridge 네트워크로 생성됨
```

### 해결
배포 스크립트를 Infrastructure as Code로 관리
```bash
# 수정된 deploy.sh
docker run -d \
  --name app \
  --network my-app-network \  # 네트워크 설정 포함
  --restart=always \
  -p 8080:8080 \
  ${IMAGE}
```

### 배운 점
- Dev-Prod Parity(개발/배포 환경 동등성) 확보의 중요성
- 자동화 스크립트는 인프라 설정까지 포함해야 완전함
- 수동 작업이 필요하면 자동화가 불완전한 것

---

## 5. WebSocket 양방향 통신 응답 전송 실패

### 문제
클라이언트에서 메시지를 보내면 서버에서 정상적으로 처리되고 DB에도 저장되지만, 클라이언트가 AI 응답을 받지 못함
```
로그: 메시지 수신 ✅, DB 저장 ✅
Postman: 응답 수신 ❌
```

### 원인
Spring WebSocket의 사용자별 메시지 전송 메커니즘 이해 부족

**시도 1: messagingTemplate 사용**
```java
messagingTemplate.convertAndSendToUser(
    userId.toString(),  // "1"
    "/queue/reply",
    response
);
```
→ Spring이 Principal의 `getName()`으로 세션을 찾는데, `JwtAuthenticationToken`에 `getName()` 미구현으로 매칭 실패

**시도 2: getName() 구현 후에도 실패**
```java
@Override
public String getName() {
    return authUser.getUserId().toString();
}
```
→ `getName()`은 구현했으나 여전히 응답 전송 실패

### 해결
WebSocket 메시지 전송 방식을 명시적 전송에서 어노테이션 기반 반환으로 변경
```java
@MessageMapping("/chat/send")
@SendToUser("/queue/reply")  // 사용자별 전송 어노테이션
public ChatResponse sendMessage(...) {
    ChatResponse response = chatbotService.processMessage(request, userId);
    return response;  // 반환값이 자동으로 구독자에게 전송됨
}
```

**변경 내용:**
- `messagingTemplate.convertAndSendToUser()` 제거
- `@SendToUser` 어노테이션 추가
- 메서드 반환 타입을 `void` → `ChatResponse`로 변경

### 배운 점
- Spring WebSocket의 `@SendToUser`는 메서드 반환값을 메시지 보낸 사용자에게 자동 전송
- `convertAndSendToUser()`는 Principal name 기반 매칭이 필요하지만, `@SendToUser`는 현재 메시지 컨텍스트 기반으로 자동 매칭
- WebSocket 양방향 통신에서는 어노테이션 기반 방식이 더 직관적이고 안정적

---
## 6. WebSocket 메시지 라우팅 destination 불일치

### 문제
상담원 기능 구현 시 사용자와 상담원 간 메시지 교환에서 서로 다른 destination 사용으로 구독 설정 복잡도 증가
```
사용자 → 상담원: /queue/messages
상담원 → 사용자: /queue/reply
사용자 ↔ AI: /queue/reply
```

### 원인
초기 설계에서 메시지 출처를 destination으로 구분하려는 의도로 서로 다른 경로 사용

**ChatbotWebSocketController:**
```java
// 사용자 메시지를 상담원에게 전달
messagingTemplate.convertAndSendToUser(
    session.getConsultant().getId().toString(),
    "/queue/messages",  // 상담원용 별도 destination
    response
);
```

**ConsultantWebSocketController:**
```java
// 상담원 메시지를 사용자에게 전달
messagingTemplate.convertAndSendToUser(
    request.userId().toString(),
    "/queue/reply",  // 일반 메시지용 destination
    response
);
```

**문제점:**
- 상담원이 2개의 destination 구독 필요 (`/queue/reply`, `/queue/messages`)
- 클라이언트 구현 복잡도 증가
- 일관성 부족

### 해결
모든 메시지를 `/queue/reply`로 통일
```java
// ChatbotWebSocketController 수정
messagingTemplate.convertAndSendToUser(
    session.getConsultant().getId().toString(),
    "/queue/reply",  // ✅ 통일
    response
);
```

**구독 설정 단순화:**
- 사용자: `/user/queue/reply` 1개만 구독
- 상담원: `/user/queue/reply` + `/user/queue/assigned` 2개 구독
- 메시지 구분은 `type` 필드 활용 (AI, CONSULTANT, SYSTEM 등)

### 배운 점
- WebSocket destination 설계 시 일관성이 중요
- 메시지 구분은 destination보다 메시지 타입 필드로 처리하는 것이 더 효율적
- 클라이언트 복잡도를 최소화하는 것이 유지보수에 유리
## 📊 성과

- **해결한 문제:** 6개
- **기술 스택:** Docker Networking, Spring Boot, WebSocket, AWS, Redis, Gemini AI
- **결과:** 완전 자동화된 배포 파이프라인 구축 및 AI/상담원 하이브리드 실시간 채팅 시스템 구현