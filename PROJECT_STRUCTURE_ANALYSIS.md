# DentalLink Project Structure Analysis

## Project Overview
DentalLink is a dental clinic reservation platform built with Spring Boot backend and vanilla JavaScript frontend. The project follows a domain-driven design pattern with clear separation of concerns.

---

## 1. ADMIN & MANAGEMENT PAGES

### Current Admin Features
**Backend Admin Controller:**
- `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/controller/UserAdminController.java`

**Admin Endpoints:**
```java
// 특정 회원 조회 (관리자 전용)
GET /api/users/{userId:[0-9]+}
- Authorization: @PreAuthorize("hasRole('ADMIN')")
- Response: UserResponse { userId, username, email, role, createdAt }

// 전체 회원 조회 (페이징)
GET /api/users?page=1&size=10&role=none
- Authorization: @PreAuthorize("hasRole('ADMIN')")
- Returns: PageResponse<UserResponse>

// 테스트용 관리자 생성
POST /api/admin
- No authorization needed (for testing only)
- Returns: UserResponse with ROLE_ADMIN

// 병원 관계자 가입 (관리자만)
POST /api/admin/hospitals/signup
- Authorization: @PreAuthorize("hasRole('ADMIN')")
- Body: UserSignupRequest { email, password, username, hospital_id }
- Returns: UserResponse with ROLE_HOSPITAL
```

**Hospital Management Endpoints:**
- `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/controller/HospitalController.java`

```java
// 병원 등록
POST /api/hospitals
- Authorization: @PreAuthorize("hasAnyRole('ADMIN')")
- Body: HospitalCreateRequest
- Returns: HospitalCreateResponse

// 병원 관계자 지정
PATCH /api/hospitals/{hospitalId}/assign/{userId}
- Authorization: @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
```

### Frontend Admin Pages
**Status:** NO dedicated admin frontend pages exist yet
- ❌ No admin dashboard
- ❌ No user management UI
- ❌ No hospital management UI
- ✅ Test admin creation available via REST API only

**Available User-Facing Pages:**
- `/index.html` - Hospital search (all users)
- `/my-page.html` - User profile management
- `/my-reservations.html` - Reservation viewing
- `/chatbot.html` - AI & consultant chat
- `/Reservation.html` - Booking page
- `/hospital.html` - Hospital detail

---

## 2. USER ROLES & AUTHENTICATION

### User Role Enum
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/enums/UserRole.java`

```java
public enum UserRole {
    ROLE_USER(Authority.USER),           // Regular user/patient
    ROLE_HOSPITAL(Authority.HOSPITAL),   // Hospital staff/manager
    ROLE_ADMIN(Authority.ADMIN)          // System administrator
}

public static class Authority {
    public static final String USER = "ROLE_USER";
    public static final String HOSPITAL = "ROLE_HOSPITAL";
    public static final String ADMIN = "ROLE_ADMIN";
}
```

### User Entity
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/entity/User.java`

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;
    private String password;
    private String username;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    // Hospital staff association
    @Column(name = "hospital_id")
    private Long hospitalId;

    // Methods
    public void assignToHospital(Long hospitalId) {
        this.hospitalId = hospitalId;
        this.userRole = UserRole.ROLE_HOSPITAL;
    }
}
```

### Authentication Flow

**Login Endpoint:**
```
POST /api/auth/login
Request: { email: string, password: string }
Response Headers:
  - Authorization: Bearer {accessToken}
  - Refresh-Token: {refreshToken}
```

**JWT Token Provider:**
- Location: JwtTokenProvider (referenced in SecurityConfig & AuthController)
- Authorization Header: `Bearer {token}`
- Refresh Token Header: `Refresh-Token: {token}`

**Authentication Filter:**
- Location: `JwtAuthenticationFilter` (injected in SecurityConfig)
- Flow: Extracts JWT from request headers → Validates → Creates JwtAuthenticationToken with AuthUser principal

**AuthUser (Security Principal):**
- Location: `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/security/AuthUser.java`
- Properties: userId, username, email, userRole
- Used in: `@AuthenticationPrincipal AuthUser authUser` annotations

### Role-Based Access Control (RBAC)

**Security Configuration:**
- Location: `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/SecurityConfig.java`

**Open Endpoints (permitAll):**
```
GET /
GET /index.html
POST /api/users/signup (registration)
POST /api/auth/login
GET /api/hospitals/** (browse hospitals)
GET /api/reservations/available-slots
GET /chatbot.html
POST /api/admin (test admin creation)
/ws/** (WebSocket - permited, but auth required in interceptor)
/swagger-ui/** (API docs)
```

**Role-Based Endpoints:**
- `@PreAuthorize("hasRole('ADMIN')")` - Admin-only operations
- `@PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")` - Admin or Hospital staff
- `@PreAuthorize("hasRole('HOSPITAL')")` - Hospital staff only

**Frontend Role Detection:**
- Stored in localStorage after login: `localStorage.getItem('user')`
- User object contains: userId, username, email, role, createdAt

---

## 3. NAVIGATION STRUCTURE

### Frontend Navigation (All Pages)
**Navigation Sidebar** (visible on all pages):
```html
<nav>
    <ul>
        <li><a href="/">🏥 병원 찾기</a></li>
        <li><a href="/my-reservations">📋 내 예약 보기</a></li>
        <li><a href="/my-page">💖 내 정보 보기</a></li>
        <li><a href="/chatbot">💬 채팅</a></li>
    </ul>
</nav>
```

**Header:**
- Logo (links to `/`)
- Search bar (hospital search)
- Login/Logout button (auth-dependent)

### Frontend Page Files
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/
├── index.html              # Hospital search & browse
├── my-page.html            # User profile (edit/password)
├── my-reservations.html    # User reservation history
├── chatbot.html            # AI & consultant chat
├── Reservation.html        # Make/manage reservation
├── hospital.html           # Hospital detail view
├── js/
│   ├── common.js           # Auth, tokens, modals, messages
│   ├── index.js            # Hospital listing & pagination
│   ├── hospital.js         # Hospital detail & reviews
│   ├── reservation.js      # Reservation logic
│   ├── chatbot.js          # WebSocket chat (AI & consultant)
│   ├── my-page.js          # Profile management
│   ├── my-reservations.js  # Reservation viewing
│   ├── commonAnimation.js  # UI animations
│   └── commonApi.js        # API utilities
└── css/                    # Styling
```

### Frontend Role-Based Navigation
**Current Implementation:**
- No role-specific navigation UI
- All users see the same navigation
- Admin features accessed only via direct API calls or URL manipulation

**Recommended Navigation Structure (for new admin dashboard):**
```
ROLE_USER
├── Home (/)
├── My Reservations (/my-reservations)
├── My Profile (/my-page)
└── Chat (/chatbot)

ROLE_HOSPITAL
├── Home (/)
├── My Reservations (/my-reservations)
├── My Profile (/my-page)
├── Chat (/chatbot)
└── Hospital Admin (/hospital-admin)  [NEW]
    ├── Manage Staff
    ├── View Reservations
    └── Manage Schedule

ROLE_ADMIN
├── Home (/)
├── My Reservations (/my-reservations)
├── My Profile (/my-page)
├── Chat (/chatbot)
└── Admin Dashboard (/admin)  [NEW]
    ├── User Management
    ├── Hospital Management
    ├── Consultant Management
    └── System Analytics
```

---

## 4. WEBSOCKET/CHAT BACKEND STRUCTURE

### WebSocket Configuration
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    // STOMP endpoint: /ws/chat
    // Application prefix: /app (client sends to /app/...)
    // Broker prefixes: /topic, /queue (server sends to /topic/... or /queue/...)
    // User destination prefix: /user (for user-specific messages)
    
    // Message paths:
    // Client sends: /app/chat/send → Server sends: /user/queue/reply
}
```

**WebSocket Endpoint:**
```
ws://localhost:8080/ws/chat
wss://yourdomain.com/ws/chat (HTTPS)
```

### Chat Controllers

**ChatbotWebSocketController (AI Chat)**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/controller/ChatbotWebSocketController.java`

```java
@Controller
public class ChatbotWebSocketController {
    
    // 1. Send message (AI or transfer to consultant)
    @MessageMapping("/chat/send")
    @SendToUser("/queue/reply")
    public ChatResponse sendMessage(
        @Payload @Valid ChatRequest request,
        SimpMessageHeaderAccessor headerAccessor
    )
    
    // 2. Close session
    @MessageMapping("/chat/close")
    public void closeSession(
        @Payload Long sessionId,
        SimpMessageHeaderAccessor headerAccessor
    )
    
    // 3. Typing indicator
    @MessageMapping("/chat/typing")
    @SendTo("/topic/typing")
    public TypingEvent handleTyping(...)
}

// Message Flow:
// Client: /app/chat/send → ChatbotService.processMessage() → 
// → Checks for consultant request keyword →
// → ChatResponse sent to /user/queue/reply
```

**ConsultantWebSocketController (Consultant Chat)**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/controller/ConsultantWebSocketController.java`

```java
@Controller
public class ConsultantWebSocketController {
    
    // 1. Send consultant message to user
    @MessageMapping("/consultant/send")
    public void sendConsultantMessage(
        @Payload ConsultantMessageRequest request,
        SimpMessageHeaderAccessor headerAccessor
    )
    // Saves message → Sends ChatResponse to user via /user/queue/reply
    
    // 2. Pick a specific waiting session
    @MessageMapping("/consultant/pick")
    public void pickNextSession(
        @Payload PickSessionRequest request,
        SimpMessageHeaderAccessor headerAccessor
    )
    // Consultant selects from waiting queue → Gets session assigned
    
    // 3. Close consultant session
    @MessageMapping("/consultant/close")
    public void closeSession(...)
    
    // REST Endpoints
    GET /api/consultant/sessions              # Get consultant's active sessions
    GET /api/consultant/queue/status          # Get waiting queue status
    GET /api/consultant/waiting-sessions      # Get list of waiting sessions
    GET /api/chat/sessions/{id}/messages      # Get session message history
}
```

### Chat Services

**ChatbotService (Main Chat Logic)**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/ChatbotService.java`

```java
@Service
@Transactional(readOnly = true)
public class ChatbotService {
    
    // Core Methods
    public ChatResponse processMessage(ChatRequest request, Long userId)
    // Main entry point - handles AI response or consultant transfer
    
    public void closeSession(Long sessionId, Long userId)
    // End chat session
    
    // Helper Methods
    private ChatSession getOrCreateSession(Long sessionId, Long userId)
    private boolean isConsultantRequestKeyword(String content)
    private ChatResponse handleConsultantMessage(...)
    private ChatResponse generateAIResponse(...) // Via GeminiApiService
}
```

**ConsultantService (Consultant Queue & Matching)**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/ConsultantService.java`

```java
@Service
@Transactional(readOnly = true)
public class ConsultantService {
    
    // Consultant Matching
    public ConsultantMatchResult transferToConsultant(Long sessionId, Long userId)
    // Try to find available consultant → If none, add to waiting queue (Redis)
    
    public Optional<ChatSession> pickSpecificSession(Long sessionId, Long consultantId)
    // Consultant accepts a specific waiting session
    
    // Message Handling
    public void sendConsultantMessage(Long sessionId, Long consultantId, String content)
    
    public void closeConsultantSession(Long sessionId, Long consultantId)
    
    // Queue Management
    public Optional<User> findAvailableConsultant()
    private void assignSessionToConsultant(ChatSession session, User consultant)
    
    // Listing
    public List<ChatSession> getConsultantSessions(Long consultantId)
    public List<WaitingSessionInfo> getWaitingSessions()
    
    // DTOs
    public record ConsultantMatchResult(
        boolean immediate,        // true if consultant assigned immediately
        Long consultantId,        // assigned consultant ID (if immediate)
        Long waitingPosition      // position in queue (if waiting)
    )
    
    public record WaitingSessionInfo(
        Long sessionId,
        String userName,
        LocalDateTime startedAt,
        Long waitingPosition
    )
}
```

**GeminiApiService (AI Response)**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/GeminiApiService.java`
- Integrates with Google Gemini API for AI responses
- Handles function calling for dynamic responses

**FunctionCallHandler**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/FunctionCallHandler.java`
- Processes function calls from Gemini AI
- Bridges AI responses with database queries

### Chat Entities & Enums

**ChatSession Entity**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/entity/ChatSession.java`

```java
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;                    // User starting chat
    
    @Enumerated(EnumType.STRING)
    private ChatMode mode;                // AI or CONSULTANT
    
    @Enumerated(EnumType.STRING)
    private SessionStatus status;         // ACTIVE, WAITING, CLOSED
    
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User consultant;              // Assigned consultant (if any)
    
    private Long waitingPosition;         // Queue position when waiting
    
    @OneToMany(mappedBy = "session")
    private List<ChatMessage> messages;   // All messages in session
}
```

**ChatMessage Entity**
```java
// Stores user, AI, and consultant messages
// Contains: sessionId, senderId, senderType (USER/AI/CONSULTANT/SYSTEM), content, timestamp
```

**Enums:**
```java
enum ChatMode { AI, CONSULTANT }
enum SessionStatus { ACTIVE, WAITING, CLOSED }
enum MessageType { USER, AI, CONSULTANT, SYSTEM }
```

### WebSocket Authentication

**WebSocketAuthChannelInterceptor**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/security/WebSocketAuthChannelInterceptor.java`

```java
// Intercepts WebSocket connections
// Extracts JWT from handshake headers
// Validates token and creates authenticated Principal
// Ensures only authenticated users can access WebSocket endpoints
```

### Chat Frontend Implementation

**Frontend Chat Client**
**Location:** `/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/chatbot.js`

```javascript
// WebSocket Connection
let stompClient = null;
let currentSessionId = null;

function connectWebSocket() {
    const wsUrl = `ws://${window.location.host}/ws/chat`;
    const ws = new WebSocket(wsUrl);
    stompClient = Stomp.over(ws);
    
    const headers = {
        'Authorization': `Bearer ${getToken()}`
    };
    
    stompClient.connect(headers, onConnected, onError);
}

// Subscribe to messages
stompClient.subscribe('/user/queue/reply', onMessageReceived);

// Send message
function sendMessage() {
    const message = {
        sessionId: currentSessionId,
        content: chatInput.value
    };
    
    stompClient.send('/app/chat/send', {}, JSON.stringify(message));
}

// Receive AI or Consultant responses
function onMessageReceived(message) {
    const response = JSON.parse(message.body);
    // Display response based on type (AI, CONSULTANT, SYSTEM)
}
```

---

## 5. REPOSITORIES & DATA PERSISTENCE

### Chat Repositories
```
ChatSessionRepository        # Find sessions by user, consultant, status
ChatMessageRepository        # Find messages by session
UserRepository              # Find users by role (e.g., consultants)
RedisTemplate               # Waiting queue management (WAITING_QUEUE_KEY)
```

### Data Models

**Chat Data Flow:**
```
User inputs message 
  ↓
ChatSession.createAISession(user)
  ↓
ChatMessage.createUserMessage(session, content)
  ↓
Gemini API processes → ChatMessage.createAIMessage(session, response)
  ↓
If consultant requested: ConsultantService.transferToConsultant()
  → Find available consultant OR add to Redis waiting queue
  ↓
WebSocket sends response via /user/queue/reply
```

---

## 6. SECURITY & AUTHORIZATION

### Endpoint Protection

**Unrestricted (Anonymous):**
- `GET /api/hospitals/**` - Browse hospitals
- `GET /api/reservations/available-slots`
- `POST /api/auth/login`
- `POST /api/users/signup`
- `/ws/**` - WebSocket (auth handled in interceptor)

**Authenticated (All Logged-in Users):**
- `GET /api/users/me`
- `PATCH /api/users/me`
- `DELETE /api/users/withdraw`
- WebSocket operations

**Role-Based:**
- `@PreAuthorize("hasRole('ADMIN')")` - Admin operations
- `@PreAuthorize("hasRole('HOSPITAL')")` - Hospital staff operations
- `@PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")` - Either role

---

## 7. KEY FILES SUMMARY

### Backend Key Files

| Category | File Path | Purpose |
|----------|-----------|---------|
| **Auth** | `AuthController.java` | Login, logout, token refresh |
| **Auth** | `SecurityConfig.java` | Spring Security configuration, CORS, JWT filter |
| **Users** | `UserAdminController.java` | Admin: user management |
| **Users** | `UserController.java` | User: signup, profile, password |
| **Users** | `User.java` | User entity with role & hospital_id |
| **Users** | `UserRole.java` | Enum: ROLE_USER, ROLE_HOSPITAL, ROLE_ADMIN |
| **Chat** | `ChatbotWebSocketController.java` | AI chat WebSocket |
| **Chat** | `ConsultantWebSocketController.java` | Consultant chat WebSocket |
| **Chat** | `ChatbotService.java` | AI chat logic & transfer |
| **Chat** | `ConsultantService.java` | Consultant matching & queue |
| **Chat** | `ChatSession.java` | Chat session entity |
| **Chat** | `ChatMessage.java` | Chat message entity |
| **Hospital** | `HospitalController.java` | Hospital CRUD & staff assignment |
| **Config** | `WebSocketConfig.java` | WebSocket/STOMP configuration |
| **Config** | `WebSocketAuthChannelInterceptor.java` | WebSocket JWT authentication |

### Frontend Key Files

| File | Purpose |
|------|---------|
| `index.html` | Hospital search & browse |
| `my-page.html` | User profile management |
| `my-reservations.html` | Reservation history |
| `chatbot.html` | Chat interface (AI + Consultant) |
| `common.js` | Token management, auth, modals |
| `chatbot.js` | WebSocket client, message handling |
| `index.js` | Hospital listing & pagination |
| `hospital.js` | Hospital details & reviews |

---

## 8. MISSING ADMIN FEATURES (TODO)

Based on analysis, the following admin/management features need to be created:

### Frontend
- [ ] **Admin Dashboard** (`/admin`) - Main admin interface
- [ ] **User Management Page** - List, view, create, delete users
- [ ] **Hospital Management Page** - CRUD hospitals, assign staff
- [ ] **Consultant Management Page** - View consultants, manage availability
- [ ] **Analytics Page** - Chat statistics, reservations, revenue
- [ ] **Role-Based Navigation** - Show different nav based on user role
- [ ] **Hospital Admin Dashboard** - For hospital staff to manage their clinic

### Backend
- [ ] **Consultant Role/Status** - Explicitly track consultant availability
- [ ] **Admin Dashboard APIs** - Summary statistics endpoints
- [ ] **Audit Logging** - Track admin actions
- [ ] **System Configuration** - Manage system settings

### Security Enhancements
- [ ] **Admin Access Logging**
- [ ] **Two-Factor Authentication** (for admins)
- [ ] **IP Whitelisting** (for admin endpoints)

---

## 9. CURRENT LIMITATIONS & NOTES

1. **No Persistent Consultant Status** - Consultants not explicitly marked as available/busy
2. **Redis-Only Waiting Queue** - Queue data not persisted to database
3. **No Audit Trail** - Admin actions not logged
4. **Basic RBAC** - Only 3 roles, no fine-grained permissions
5. **No Admin UI** - Admin functions accessible only via API
6. **Single Admin Account** - No admin user management
7. **No Analytics** - No built-in dashboards or reports

---

## 10. PROJECT STRUCTURE DIAGRAM

```
dentallink/
├── src/main/java/com/dentallink/
│   ├── common/
│   │   ├── config/              # Spring configurations
│   │   │   ├── SecurityConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── ...
│   │   ├── security/            # JWT & WebSocket auth
│   │   ├── exception/           # Error handling
│   │   └── response/            # API response wrappers
│   └── domain/                  # Business logic (DDD)
│       ├── auth/                # Authentication
│       │   ├── controller/
│       │   ├── service/
│       │   └── dto/
│       ├── user/                # User management
│       │   ├── controller/      # UserController, UserAdminController
│       │   ├── entity/          # User.java
│       │   ├── enums/           # UserRole.java
│       │   └── service/
│       ├── chatbot/             # Chat system
│       │   ├── controller/      # ChatbotWebSocketController, ConsultantWebSocketController
│       │   ├── service/         # ChatbotService, ConsultantService
│       │   ├── entity/          # ChatSession, ChatMessage
│       │   └── enums/           # ChatMode, SessionStatus
│       ├── hospital/            # Hospital management
│       ├── reservation/         # Reservations
│       ├── review/              # Reviews
│       └── ...
├── src/main/resources/
│   └── static/                  # Frontend files
│       ├── index.html
│       ├── chatbot.html
│       ├── my-page.html
│       ├── js/                  # JavaScript files
│       │   ├── common.js
│       │   ├── chatbot.js
│       │   └── ...
│       └── css/                 # Stylesheets
└── build.gradle                 # Gradle dependencies
```

---

## QUICK START: Adding Admin Dashboard

To create an admin dashboard:

1. **Create Admin Page:** `/admin.html` with navigation & content areas
2. **Create Admin Services:** API calls for user/hospital management
3. **Add Route:** Add `/admin` and `/admin.html` to SecurityConfig permitAll
4. **Create Components:**
   - UserManagement.js - User CRUD
   - HospitalManagement.js - Hospital CRUD
   - ConsultantManagement.js - Consultant queue management
5. **Update Navigation:** Conditionally show admin link if `userRole === 'ROLE_ADMIN'`
6. **Add to common.js:** Helper functions for admin-specific operations

