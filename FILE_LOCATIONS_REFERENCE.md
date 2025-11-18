# DentalLink Project - File Locations Reference

This document provides absolute file paths for all key project files related to admin/management features, authentication, chat, and navigation.

---

## AUTHENTICATION & SECURITY

### JWT & Token Management
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/utility/JwtAuthenticationFilter.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/utility/JwtTokenProvider.java
```

### Security Configuration
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/SecurityConfig.java
    └─ Contains: CORS, JWT filter chain, role-based endpoint security
    
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/security/JwtAuthenticationToken.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/security/WebSocketAuthChannelInterceptor.java
```

---

## USER MANAGEMENT (AUTHENTICATION & ROLES)

### Controllers
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/auth/controller/AuthController.java
    └─ POST /api/auth/login
    └─ POST /api/auth/refresh-token
    └─ POST /api/auth/logout

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/controller/UserController.java
    └─ POST /api/users/signup
    └─ PATCH /api/users/me
    └─ PUT /api/users/password
    └─ DELETE /api/users/withdraw
    └─ GET /api/users/me

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/controller/UserAdminController.java
    └─ GET /api/users/{userId}                  [ADMIN]
    └─ GET /api/users?page=1&size=10&role=     [ADMIN]
    └─ POST /api/admin                          [Test only]
    └─ POST /api/admin/hospitals/signup         [ADMIN]
```

### Entities & Enums
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/entity/User.java
    └─ Fields: id, email, password, username, userRole, hospitalId
    └─ Methods: assignToHospital(), update(), updatePassword()

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/enums/UserRole.java
    └─ ROLE_USER (regular patient)
    └─ ROLE_HOSPITAL (hospital staff)
    └─ ROLE_ADMIN (system administrator)

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/security/AuthUser.java
    └─ Security principal: userId, username, email, userRole
```

### Services
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/auth/service/AuthService.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/service/UserInternalService.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/service/UserExternalService.java
```

### DTOs
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/request/UserSignupRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/request/UserUpdateRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/request/UserUpdatePasswordRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/request/UserDeleteRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/dto/response/UserResponse.java
    └─ Fields: userId, username, email, role, createdAt
```

### Repositories
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/user/repository/UserRepository.java
```

---

## HOSPITAL MANAGEMENT

### Controller
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/controller/HospitalController.java
    └─ GET /api/hospitals (list, paginated)
    └─ GET /api/hospitals/{id} (detail)
    └─ POST /api/hospitals (create) [ADMIN]
    └─ PATCH /api/hospitals/{hospitalId}/assign/{userId} [ADMIN, HOSPITAL]
```

### Entities
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/entity/Hospital.java
```

### Services
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/service/HospitalInternalService.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/service/HospitalExternalService.java
```

### DTOs
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/dto/request/HospitalCreateRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/dto/response/HospitalListResponse.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/dto/response/HospitalDetailResponse.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/dto/response/HospitalCreateResponse.java
```

### Repositories
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/hospital/repository/HospitalRepository.java
```

---

## CHAT & WEBSOCKET (CHATBOT & CONSULTANT)

### WebSocket Configuration
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/WebSocketConfig.java
    └─ Endpoint: /ws/chat
    └─ Application prefix: /app
    └─ Broker: /topic, /queue
    └─ User destination: /user

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/security/WebSocketAuthChannelInterceptor.java
    └─ Intercepts WebSocket connections
    └─ Validates JWT tokens
    └─ Sets up authentication principal
```

### Controllers
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/controller/ChatbotWebSocketController.java
    └─ @MessageMapping("/chat/send")      [User → AI/Consultant]
    └─ @MessageMapping("/chat/close")     [Close session]
    └─ @MessageMapping("/chat/typing")    [Typing indicator]

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/controller/ConsultantWebSocketController.java
    └─ @MessageMapping("/consultant/send")       [Consultant → User]
    └─ @MessageMapping("/consultant/pick")       [Accept session]
    └─ @MessageMapping("/consultant/close")      [End session]
    └─ GET /api/consultant/sessions              [REST API]
    └─ GET /api/consultant/waiting-sessions      [REST API]
    └─ GET /api/consultant/queue/status          [REST API]
    └─ GET /api/chat/sessions/{id}/messages      [REST API]
```

### Services
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/ChatbotService.java
    └─ processMessage()          [Main chat logic]
    └─ closeSession()            [End session]
    └─ handleConsultantMessage() [Consultant message handling]

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/ConsultantService.java
    └─ transferToConsultant()    [Consultant matching]
    └─ pickSpecificSession()     [Accept specific session]
    └─ sendConsultantMessage()   [Consultant sends message]
    └─ closeConsultantSession()  [Consultant ends session]
    └─ getConsultantSessions()   [List consultant's sessions]
    └─ getWaitingSessions()      [Get waiting queue]

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/GeminiApiService.java
    └─ Integrates with Google Gemini API

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/service/FunctionCallHandler.java
    └─ Processes AI function calls
```

### Entities & Enums
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/entity/ChatSession.java
    └─ Fields: id, user, mode, status, consultant, waitingPosition, messages
    └─ Methods: startAISession(), transferToConsultant(), moveToWaitingPosition()

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/entity/ChatMessage.java
    └─ Fields: id, session, sender, type, content, sentAt

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/enums/ChatMode.java
    └─ AI, CONSULTANT

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/enums/SessionStatus.java
    └─ ACTIVE, WAITING, CLOSED

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/enums/MessageType.java
    └─ USER, AI, CONSULTANT, SYSTEM
```

### DTOs
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/dto/ChatRequest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/dto/ChatResponse.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/dto/SessionResponse.java
```

### Repositories
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/repository/ChatSessionRepository.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/domain/chatbot/repository/ChatMessageRepository.java
```

---

## FRONTEND - HTML PAGES

### Navigation & Main Pages
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/index.html
    └─ Hospital search & browse (all users)
    └─ Header with navigation and search
    └─ Login/Signup modals

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/my-page.html
    └─ User profile management
    └─ Edit username/email, change password

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/my-reservations.html
    └─ User reservation history
    └─ View, cancel, review reservations

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/chatbot.html
    └─ AI & Consultant chat interface
    └─ WebSocket-based real-time messaging

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/Reservation.html
    └─ Make new reservation

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/hospital.html
    └─ Hospital detail view
```

---

## FRONTEND - JAVASCRIPT

### Core & Common
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/common.js
    └─ Token management (getToken, setToken, removeToken)
    └─ User management (getUser, setUser)
    └─ Auth checks (isLoggedIn, requireLogin)
    └─ Message display (showMessage)
    └─ Modal controls (showLoginModal, showSignupModal, etc.)
    └─ UI updates (updateUIAfterLogin, updateUIAfterLogout)
    └─ Validation (validatePassword)

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/commonApi.js
    └─ API utility functions
    └─ Fetch wrappers

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/commonAnimation.js
    └─ UI animations
```

### Page-Specific JavaScript
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/index.js
    └─ Hospital listing & pagination
    └─ Hospital grid rendering
    └─ Favorite management

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/hospital.js
    └─ Hospital detail page logic
    └─ Reviews display
    └─ Reservation redirect

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/reservation.js
    └─ Reservation creation logic
    └─ Date/time selection
    └─ Service selection

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/chatbot.js
    └─ WebSocket connection (STOMP)
    └─ Message sending/receiving
    └─ AI & Consultant chat handling
    └─ Session management
    └─ Typing indicators

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/my-page.js
    └─ Profile editing
    └─ Password change
    └─ Account deletion

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/my-reservations.js
    └─ Reservation listing
    └─ Cancellation
    └─ Review submission
```

---

## FRONTEND - STYLESHEETS

```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/common.css
    └─ Base styles, layout, navigation, modals
    └─ Responsive design

/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/index.css
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/hospital.css
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/my-page.css
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/my-reservations.css
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/chatbot.css
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/css/reservation.css
```

---

## CORE CONFIGURATION FILES

### Application Configuration
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/SecurityConfig.java
    └─ Spring Security setup, JWT filter, CORS, role-based access

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/WebSocketConfig.java
    └─ WebSocket/STOMP configuration

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/WebMvcConfig.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/RedisConfig.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/RedisCacheConfig.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/GeminiConfig.java
    └─ Google Gemini API configuration (needs .env setup)

/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/JpaAuditingConfig.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/SwaggerConfig.java
```

### Build Configuration
```
/Users/hyuncles/Desktop/sparta/dentallink/build.gradle
    └─ Dependencies, plugins, build configuration
```

---

## COMMON UTILITIES & HELPERS

### Exception Handling
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/exception/GlobalException.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/exception/GlobalExceptionHandler.java
```

### Response Wrappers
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/response/CommonApiResponse.java
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/response/PageResponse.java
```

### Entity Base Classes
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/entity/BaseEntity.java
    └─ createdAt, updatedAt (auto-managed by JpaAuditing)
```

---

## TESTING

### Test Files
```
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/auth/service/AuthServiceTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/user/service/UserInternalServiceTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/user/service/UserExternalServiceTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/chatbot/service/ChatbotServiceTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/chatbot/service/ConsultantServiceTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/hospital/controller/HospitalControllerTest.java
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/domain/reservation/service/ReservationInternalServiceTest.java
```

---

## DOCUMENTATION

### Generated Documentation
```
/Users/hyuncles/Desktop/sparta/dentallink/PROJECT_STRUCTURE_ANALYSIS.md
    └─ Comprehensive project structure analysis (THIS DOCUMENT CREATED)

/Users/hyuncles/Desktop/sparta/dentallink/ADMIN_FEATURES_ROADMAP.md
    └─ Admin features development roadmap

/Users/hyuncles/Desktop/sparta/dentallink/EXPLORATION_SUMMARY.md
    └─ Previously generated exploration summary
```

### Project Documentation
```
/Users/hyuncles/Desktop/sparta/dentallink/README.md
/Users/hyuncles/Desktop/sparta/dentallink/HELP.md
/Users/hyuncles/Desktop/sparta/dentallink/TROUBLESHOOTING.md
```

---

## KEY ABSOLUTE PATHS SUMMARY

### Backend Base Path
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/
```

### Frontend Base Path
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/
```

### Test Base Path
```
/Users/hyuncles/Desktop/sparta/dentallink/src/test/java/com/dentallink/
```

### Configuration Base Path
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/
```

---

## QUICK NAVIGATION TIPS

1. **To find all admin-related files:**
   ```bash
   grep -r "ADMIN\|Admin\|admin" /Users/hyuncles/Desktop/sparta/dentallink/src/main/java --include="*.java"
   ```

2. **To find all role-based authorization:**
   ```bash
   grep -r "@PreAuthorize" /Users/hyuncles/Desktop/sparta/dentallink/src/main/java --include="*.java"
   ```

3. **To find all WebSocket handlers:**
   ```bash
   grep -r "@MessageMapping" /Users/hyuncles/Desktop/sparta/dentallink/src/main/java --include="*.java"
   ```

4. **To find all frontend JavaScript files:**
   ```bash
   find /Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js -name "*.js"
   ```

5. **To find all configuration classes:**
   ```bash
   find /Users/hyuncles/Desktop/sparta/dentallink/src/main/java -name "*Config.java"
   ```

