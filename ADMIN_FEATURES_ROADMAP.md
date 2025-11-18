# Admin Features & Management System Roadmap

## Overview
This document outlines the current state of admin/management features in DentalLink and provides a roadmap for development.

---

## 1. EXISTING ADMIN CAPABILITIES

### Backend APIs (Implemented)
```
ADMIN ENDPOINTS AVAILABLE:
├── User Management
│   ├── GET /api/users/{userId}              ✓ Get specific user
│   ├── GET /api/users?page=1&size=10        ✓ List all users with pagination
│   ├── POST /api/admin                      ✓ Create test admin account
│   └── POST /api/admin/hospitals/signup     ✓ Create hospital staff user
│
├── Hospital Management
│   ├── POST /api/hospitals                  ✓ Create hospital
│   ├── PATCH /api/hospitals/{id}/assign/{userId}  ✓ Assign staff to hospital
│   └── GET /api/hospitals                   ✓ List hospitals
│
└── Consultant Management
    ├── GET /api/consultant/sessions         ✓ Get consultant's active sessions
    ├── GET /api/consultant/waiting-sessions ✓ Get waiting queue
    └── GET /api/consultant/queue/status     ✓ Get queue statistics
```

### Authorization Pattern
```java
// Admin-only endpoints use:
@PreAuthorize("hasRole('ADMIN')")

// Hospital staff + Admin can access:
@PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")

// All authentication checks are enforced at:
/Users/hyuncles/Desktop/sparta/dentallink/src/main/java/com/dentallink/common/config/SecurityConfig.java
```

---

## 2. MISSING ADMIN FRONTEND

### Current Situation
- No admin-facing UI/Dashboard exists
- Admin can only access features via REST API calls
- No admin navigation in frontend
- No role-based page routing

### Required Pages to Create

#### 2.1 Admin Dashboard (`/admin.html`)
```html
Main admin landing page with:
- Quick stats cards (total users, hospitals, consultants, active chats)
- Navigation to different admin sections
- System health indicators
- Recent activity log
```

**Frontend file to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/admin.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/admin-dashboard.js
```

#### 2.2 User Management Page (`/admin/users`)
```html
Features needed:
- User list table (paginated)
  - Columns: ID, Email, Username, Role, Created Date, Actions
  - Search/filter by role, email
  - Sort by creation date, role
  
- User detail view
  - View user profile
  - Change user role
  - Reset password (admin action)
  - Delete user (with confirmation)
  
- Create user modal
  - Email, username, password, role selection
  - Assign to hospital if ROLE_HOSPITAL

- User edit form
  - Update username/email
  - Change role
  - Lock/unlock account (future)
```

**Frontend files to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/admin-users.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/admin-users.js
```

**New Backend APIs Required:**
```java
// User deletion (if not implemented)
DELETE /api/users/{userId}
@PreAuthorize("hasRole('ADMIN')")

// Change user role
PATCH /api/users/{userId}/role
@PreAuthorize("hasRole('ADMIN')")
Body: { newRole: "ROLE_USER" | "ROLE_HOSPITAL" | "ROLE_ADMIN" }

// User search/filter
GET /api/users/search?email=...&role=...
@PreAuthorize("hasRole('ADMIN')")
```

#### 2.3 Hospital Management Page (`/admin/hospitals`)
```html
Features needed:
- Hospital list table
  - Columns: ID, Name, Address, Doctor, Status, Staff Count, Actions
  - Search by name, location, doctor
  - Sort by name, creation date
  
- Hospital detail view
  - View all hospital info
  - Edit hospital details
  - List assigned staff members
  - Add/remove staff
  - View hospital's reservations
  - View hospital's reviews
  
- Create hospital modal
  - Hospital name, address, phone, doctor name
  - Opening hours setup
  - Services offered

- Hospital edit form
  - Update basic info
  - Update operating hours
  - Update contact info
  - Manage assigned staff
```

**Frontend files to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/admin-hospitals.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/admin-hospitals.js
```

**New Backend APIs Required:**
```java
// Update hospital
PUT /api/hospitals/{hospitalId}
@PreAuthorize("hasRole('ADMIN')")

// Delete hospital
DELETE /api/hospitals/{hospitalId}
@PreAuthorize("hasRole('ADMIN')")

// List hospital staff
GET /api/hospitals/{hospitalId}/staff
@PreAuthorize("hasRole('ADMIN')")

// Remove staff from hospital
DELETE /api/hospitals/{hospitalId}/staff/{userId}
@PreAuthorize("hasRole('ADMIN')")
```

#### 2.4 Consultant Management Page (`/admin/consultants`)
```html
Features needed:
- Consultant list
  - Columns: ID, Name, Email, Status (online/offline), Active Sessions, Avg Response Time
  - Real-time status indicators
  - Filter by status
  
- Consultant detail
  - Current sessions
  - Statistics (total chats, avg duration, rating)
  - Chat history
  
- Consultant controls
  - Set availability (online/offline)
  - Assign to specific categories (optional)
  - View performance metrics
  
- Queue management
  - View current waiting queue
  - Waiting session details
  - Manual session assignment
  - Queue analytics (wait times, distribution)
```

**Frontend files to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/admin-consultants.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/admin-consultants.js
```

**New Backend APIs Required:**
```java
// Get all consultants
GET /api/consultants
@PreAuthorize("hasRole('ADMIN')")
Returns: List<ConsultantResponse> { id, name, email, status, activeSessionCount, stats }

// Get consultant details
GET /api/consultants/{consultantId}
@PreAuthorize("hasRole('ADMIN')")

// Set consultant availability
PATCH /api/consultants/{consultantId}/availability
@PreAuthorize("hasRole('ADMIN')")
Body: { available: true/false }

// Get consultant statistics
GET /api/consultants/{consultantId}/stats
@PreAuthorize("hasRole('ADMIN')")
Returns: { totalChats, avgDuration, avgRating, responseTime }

// Manually assign session to consultant
POST /api/consultants/{consultantId}/assign-session/{sessionId}
@PreAuthorize("hasRole('ADMIN')")
```

#### 2.5 Analytics & Reports Page (`/admin/analytics`)
```html
Charts & metrics:
- User metrics
  - Total users by role (pie chart)
  - New users over time (line chart)
  - User retention
  
- Chat statistics
  - Total chats (AI vs Consultant)
  - Chat duration distribution
  - Consultant response times
  - User satisfaction ratings
  
- Hospital metrics
  - Hospitals by region (map)
  - Reservations per hospital
  - Average reviews by hospital
  
- System health
  - Chat queue status
  - Average wait times
  - System uptime
  - Error rates
```

**Frontend files to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/admin-analytics.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/admin-analytics.js
```

**New Backend APIs Required:**
```java
// Get dashboard statistics
GET /api/admin/dashboard/stats
@PreAuthorize("hasRole('ADMIN')")
Returns: {
  totalUsers, newUsersThisMonth,
  totalHospitals, totalChats, avgWaitTime,
  activeConsultants, chatsSolvedByAI, chatsSolvedByConsultant
}

// Get detailed analytics
GET /api/admin/analytics/users
GET /api/admin/analytics/chats
GET /api/admin/analytics/hospitals
GET /api/admin/analytics/reservations
@PreAuthorize("hasRole('ADMIN')")
```

---

## 3. HOSPITAL STAFF DASHBOARD (ROLE_HOSPITAL)

### Required Pages

#### 3.1 Hospital Admin Dashboard (`/hospital-admin`)
```html
Hospital staff view (ROLE_HOSPITAL):
- Hospital info overview
- Staff management
  - List team members
  - Add/remove staff
  - Manage staff roles
  
- Reservations dashboard
  - Upcoming reservations
  - Cancellations
  - No-shows
  
- Schedule management
  - Operating hours
  - Available time slots
  - Blocking periods (breaks, closures)
  
- Services & pricing
  - List services offered
  - Update pricing
  - Add/remove services
  
- Reviews & feedback
  - Recent reviews
  - Average rating
  - Responding to reviews
```

**Frontend files to create:**
```
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/hospital-admin.html
/Users/hyuncles/Desktop/sparta/dentallink/src/main/resources/static/js/hospital-admin.js
```

**Backend APIs required:**
```java
// Hospital staff can CRUD their own hospital data
GET /api/hospitals/my-hospital
@PreAuthorize("hasRole('HOSPITAL')")

PATCH /api/hospitals/{myHospitalId}
@PreAuthorize("hasRole('HOSPITAL')")

GET /api/hospitals/{myHospitalId}/staff
@PreAuthorize("hasRole('HOSPITAL')")

POST /api/hospitals/{myHospitalId}/staff
@PreAuthorize("hasRole('HOSPITAL')")

DELETE /api/hospitals/{myHospitalId}/staff/{userId}
@PreAuthorize("hasRole('HOSPITAL')")
```

---

## 4. FRONTEND NAVIGATION UPDATES

### Current Navigation (All Users See Same Nav)
```html
<nav>
    <li><a href="/">🏥 병원 찾기</a></li>
    <li><a href="/my-reservations">📋 내 예약 보기</a></li>
    <li><a href="/my-page">💖 내 정보 보기</a></li>
    <li><a href="/chatbot">💬 채팅</a></li>
</nav>
```

### NEW: Role-Based Navigation (to implement)

**For ROLE_USER (Regular Patient):**
```html
<nav>
    <li><a href="/">🏥 병원 찾기</a></li>
    <li><a href="/my-reservations">📋 내 예약 보기</a></li>
    <li><a href="/my-page">💖 내 정보 보기</a></li>
    <li><a href="/chatbot">💬 채팅</a></li>
</nav>
```

**For ROLE_HOSPITAL (Hospital Staff):**
```html
<nav>
    <li><a href="/">🏥 병원 찾기</a></li>
    <li><a href="/my-reservations">📋 내 예약 보기</a></li>
    <li><a href="/my-page">💖 내 정보 보기</a></li>
    <li><a href="/chatbot">💬 채팅</a></li>
    <li><a href="/hospital-admin">🏢 병원 관리</a></li>  <!-- NEW -->
</nav>
```

**For ROLE_ADMIN (System Administrator):**
```html
<nav>
    <li><a href="/">🏥 병원 찾기</a></li>
    <li><a href="/my-reservations">📋 내 예약 보기</a></li>
    <li><a href="/my-page">💖 내 정보 보기</a></li>
    <li><a href="/chatbot">💬 채팅</a></li>
    <li><a href="/admin">⚙️ 관리자</a></li>  <!-- NEW -->
        <ul>
            <li><a href="/admin/users">👥 회원 관리</a></li>
            <li><a href="/admin/hospitals">🏥 병원 관리</a></li>
            <li><a href="/admin/consultants">💼 상담원 관리</a></li>
            <li><a href="/admin/analytics">📊 분석</a></li>
        </ul>
</nav>
```

**Implementation in common.js:**
```javascript
function renderNavigation(userRole) {
    const baseNav = [
        { href: '/', label: '🏥 병원 찾기' },
        { href: '/my-reservations', label: '📋 내 예약 보기' },
        { href: '/my-page', label: '💖 내 정보 보기' },
        { href: '/chatbot', label: '💬 채팅' }
    ];
    
    let navItems = baseNav;
    
    if (userRole === 'ROLE_HOSPITAL') {
        navItems.push({ href: '/hospital-admin', label: '🏢 병원 관리' });
    } else if (userRole === 'ROLE_ADMIN') {
        navItems.push({ 
            href: '/admin', 
            label: '⚙️ 관리자',
            submenu: [
                { href: '/admin/users', label: '👥 회원 관리' },
                { href: '/admin/hospitals', label: '🏥 병원 관리' },
                { href: '/admin/consultants', label: '💼 상담원 관리' },
                { href: '/admin/analytics', label: '📊 분석' }
            ]
        });
    }
    
    renderNavUI(navItems);
}
```

---

## 5. BACKEND ENHANCEMENTS NEEDED

### 5.1 New Entities/Features Required

**ConsultantStatus Entity** (to track consultant availability)
```java
@Entity
public class ConsultantStatus {
    @Id
    private Long consultantId;
    
    @Enumerated(EnumType.STRING)
    private OnlineStatus status;  // ONLINE, OFFLINE, ON_BREAK
    
    private LocalDateTime lastStatusChange;
    private Integer activeSessionCount;
    private LocalDateTime lastActiveTime;
}

enum OnlineStatus { ONLINE, OFFLINE, ON_BREAK }
```

**AuditLog Entity** (for admin action tracking)
```java
@Entity
public class AuditLog {
    @Id
    @GeneratedValue
    private Long id;
    
    private Long adminId;
    private String action;  // CREATE_USER, DELETE_USER, etc.
    private String entityType;  // User, Hospital, etc.
    private Long entityId;
    private String changes;  // JSON of what changed
    private LocalDateTime timestamp;
}
```

### 5.2 Service Layer Updates

**AdminService** (new service for admin operations)
```java
@Service
public class AdminService {
    // User management
    public PageResponse<UserResponse> getAllUsers(Pageable pageable, String role);
    public void deleteUser(Long userId);
    public void changeUserRole(Long userId, UserRole newRole);
    
    // Hospital management  
    public PageResponse<HospitalResponse> getAllHospitals(Pageable pageable);
    public void updateHospital(Long id, HospitalUpdateRequest request);
    public void deleteHospital(Long id);
    
    // Analytics
    public AdminDashboardStats getDashboardStats();
    public PageResponse<ChatAnalytics> getChatAnalytics(Pageable pageable);
}
```

**ConsultantStatusService** (new service)
```java
@Service
public class ConsultantStatusService {
    public void setConsultantOnline(Long consultantId);
    public void setConsultantOffline(Long consultantId);
    public ConsultantStatus getConsultantStatus(Long consultantId);
    public List<ConsultantResponse> getAvailableConsultants();
}
```

### 5.3 Controller Updates

**Create AdminController** (new REST controller)
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    // User management endpoints
    // Hospital management endpoints
    // Analytics endpoints
    // Consultant management endpoints
}
```

**Create HospitalAdminController** (for hospital staff)
```java
@RestController
@RequestMapping("/api/hospital-admin")
@PreAuthorize("hasRole('HOSPITAL')")
public class HospitalAdminController {
    // Hospital staff management endpoints
    // Schedule management endpoints
    // Service management endpoints
}
```

---

## 6. SECURITY CONSIDERATIONS

### Authorization Rules
```java
// Admin-only endpoints
@PreAuthorize("hasRole('ADMIN')")

// Hospital staff + Admin can manage their hospital
@PreAuthorize("hasRole('HOSPITAL') && #hospitalId == authentication.principal.hospitalId OR hasRole('ADMIN')")

// Only hospital staff of that hospital can manage
@PreAuthorize("@hospitalSecurityService.isHospitalStaff(#hospitalId, authentication.principal.userId)")

// Consultant can only see their own sessions
@PreAuthorize("@consultantSecurityService.isConsultant(authentication.principal.userId) && #consultantId == authentication.principal.userId OR hasRole('ADMIN')")
```

### API Security Headers
```java
// In SecurityConfig, add for admin endpoints:
.requestMatchers("/api/admin/**").requiresSecure()  // HTTPS only
.requestMatchers("/api/admin/**").requiresChannel().requiresSecure()
```

---

## 7. IMPLEMENTATION PRIORITY

### Phase 1: Core Admin Dashboard (Weeks 1-2)
- [ ] Create `/admin.html` with basic dashboard
- [ ] User management page with list/view/delete
- [ ] Update navigation to show admin link for admins
- [ ] Add role-based navigation logic to common.js

### Phase 2: Hospital Management (Weeks 2-3)
- [ ] Hospital management page (CRUD)
- [ ] Hospital staff assignment UI
- [ ] Create hospital admin dashboard for hospital staff

### Phase 3: Consultant Management (Week 3-4)
- [ ] Consultant management page
- [ ] Consultant availability status tracking
- [ ] Consultant statistics display

### Phase 4: Analytics & Reporting (Week 4-5)
- [ ] Analytics dashboard
- [ ] Chart library integration (Chart.js or similar)
- [ ] Analytics APIs

### Phase 5: Polish & Testing (Week 5-6)
- [ ] Role-based access control testing
- [ ] Performance optimization
- [ ] Documentation

---

## 8. FILE CHECKLIST FOR DEVELOPMENT

### Files to Create

Frontend:
- [ ] `/admin.html` - Main admin dashboard
- [ ] `/admin-users.html` - User management
- [ ] `/admin-hospitals.html` - Hospital management
- [ ] `/admin-consultants.html` - Consultant management
- [ ] `/admin-analytics.html` - Analytics
- [ ] `/hospital-admin.html` - Hospital staff dashboard
- [ ] `/js/admin-dashboard.js`
- [ ] `/js/admin-users.js`
- [ ] `/js/admin-hospitals.js`
- [ ] `/js/admin-consultants.js`
- [ ] `/js/admin-analytics.js`
- [ ] `/js/hospital-admin.js`
- [ ] `/css/admin.css` - Admin-specific styles

Backend:
- [ ] `AdminController.java`
- [ ] `HospitalAdminController.java`
- [ ] `AdminService.java`
- [ ] `ConsultantStatusService.java`
- [ ] `ConsultantStatus.java` (entity)
- [ ] `AuditLog.java` (entity)
- [ ] `ConsultantStatusRepository.java`
- [ ] `AuditLogRepository.java`

### Files to Modify

Frontend:
- [ ] `common.js` - Add role-based navigation, admin helper functions
- [ ] `index.html` - Add admin nav link

Backend:
- [ ] `SecurityConfig.java` - Add admin routes, security rules
- [ ] `User.java` - No changes needed (already has role)
- [ ] `UserAdminController.java` - Already exists, may need enhancements

---

## 9. TESTING REQUIREMENTS

### Unit Tests
- [ ] AdminService tests
- [ ] ConsultantStatusService tests
- [ ] Admin controller tests

### Integration Tests
- [ ] Admin user management flow
- [ ] Hospital management flow
- [ ] Role-based access control

### Frontend Tests
- [ ] Navigation rendering based on role
- [ ] Admin dashboard page rendering
- [ ] User list pagination
- [ ] Hospital CRUD operations

### Security Tests
- [ ] Non-admins cannot access admin endpoints
- [ ] Hospital staff can only manage their hospital
- [ ] Consultants can only access their own data

---

## 10. DEPLOYMENT NOTES

### Environment Variables
```
# No new environment variables needed initially
# Admin features use existing auth/security setup
```

### Database Migrations
```sql
-- If adding ConsultantStatus tracking
CREATE TABLE consultant_status (
    consultant_id BIGINT PRIMARY KEY,
    status VARCHAR(20),
    last_status_change TIMESTAMP,
    active_session_count INT,
    last_active_time TIMESTAMP
);

-- If adding AuditLog
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT,
    action VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    changes TEXT,
    timestamp TIMESTAMP
);
```

### API Documentation Updates
- Update Swagger/OpenAPI documentation
- Document new admin endpoints
- Update authorization annotations

---

## Summary

The DentalLink system has a solid foundation with existing role-based access control and backend APIs. The main gap is the absence of a frontend admin interface. This roadmap provides a structured approach to build out comprehensive admin and management interfaces while maintaining security and usability.

