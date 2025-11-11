# DentalLink Chatbot Domain - Exploration Complete

## Summary

Successfully explored and documented the complete consultant connection system in the DentalLink chatbot domain. Three comprehensive documentation files have been created.

---

## Documentation Files Created

### 1. CONSULTANT_CONNECTION_ANALYSIS.md (24 KB)
**Comprehensive technical analysis covering:**
- Architecture overview and directory structure
- All API endpoints (WebSocket and REST)
- Detailed consultant connection flow with diagrams
- Data models (ChatSession, ChatMessage) with state transitions
- Error handling (13 ChatbotErrorCode enums)
- Keyword detection system (32+ keywords)
- Rate limiting implementation (15 req/min global, 10 req/min per user)
- Redis queue management and operations
- Complete flow examples with multiple scenarios
- Transaction and consistency handling
- Security and authorization checks
- Performance optimizations
- File paths reference table

### 2. CODE_SNIPPETS_REFERENCE.md (20 KB)
**Production-ready code snippets including:**
- ConsultantService key methods with inline comments
- ChatbotService core logic and keyword detection
- WebSocket controller handlers (ConsultantWebSocketController, ChatbotWebSocketController)
- Entity models and state management
- DTOs with all fields
- Error codes enum
- All enums (ChatMode, SessionStatus, MessageType)
- Repository interface queries
- Authentication helper methods

### 3. CONSULTANT_CONNECTION_QUICK_REFERENCE.md (9 KB)
**Quick lookup guide with:**
- File role mapping table
- 1-minute flow summary (simplified diagram)
- State transitions visual
- Redis queue structure
- Error codes lookup table
- WebSocket endpoint quick reference
- REST API endpoints
- Key constants and limits
- Keyword list
- Performance notes
- Transaction boundaries
- Testing checklist
- Common issues and solutions

---

## Key Findings

### Architecture Highlights

1. **Two-Tier System:**
   - AI Chatbot Mode: Gemini-powered responses
   - Consultant Mode: Human support escalation

2. **Real-time Communication:**
   - WebSocket for all real-time interactions
   - No HTTP polling required
   - User-specific message routing

3. **Queue Management:**
   - Redis FIFO queue for waiting sessions
   - O(1) queue operations
   - Automatic position tracking

4. **Smart Escalation Triggers:**
   - Keyword detection (32 keywords in Korean + English)
   - Rate limiting (automatic transfer when limits exceeded)
   - Manual user request support

### Main Controllers

**ChatbotWebSocketController:**
- `/app/chat/send` - User sends message to AI
- `/app/chat/close` - User closes session

**ConsultantWebSocketController:**
- `/app/consultant/send` - Consultant sends message
- `/app/consultant/pick` - Consultant requests next session
- `/app/consultant/close` - Consultant closes session
- REST endpoints for queue monitoring

### Core Services

**ChatbotService:**
- `processMessage()` - Main entry point
- Keyword detection with 32+ keywords
- Rate limiting (dual-layer: global + per-user)
- AI response generation via Gemini

**ConsultantService:**
- `transferToConsultant()` - Connection logic
- `pickNextWaitingSession()` - Queue assignment
- `findAvailableConsultant()` - Availability check
- Queue position management

### Data Models

**ChatSession:**
- States: ACTIVE, WAITING, CLOSED
- Modes: AI, CONSULTANT
- Nullable consultant field
- Waiting position tracking

**ChatMessage:**
- Types: USER, AI, CONSULTANT, SYSTEM
- Supports function call tracking
- Indexed on (session_id, sent_at)

### Error Handling

13 specific error codes for:
- Session management (NOT_FOUND, ALREADY_CLOSED, NOT_ACTIVE)
- Messages (EMPTY, TOO_LONG)
- Consultants (NOT_FOUND, NOT_CONSULTANT_SESSION, UNAUTHORIZED_ACCESS)
- API (GEMINI_API_ERROR, RATE_LIMIT_EXCEEDED)
- Authorization (UNAUTHORIZED_ACCESS)

---

## Flow Summary

### User Requests Consultant

```
1. User sends message
2. System detects keyword OR rate limit exceeded
3. transferToConsultant() called
4. Find available consultant (max 3 sessions per consultant)
5. If available: Immediate connection
6. If not: Add to Redis queue, return position number
7. User sees waiting position or connection confirmation
```

### Consultant Picks Session

```
1. Consultant clicks "Pick next session"
2. pickNextWaitingSession() called
3. Pop first session from Redis queue (FIFO)
4. Validate session is WAITING status
5. Update session: CONSULTANT mode + ACTIVE status
6. Assign consultant to session
7. Notify both parties
8. Update remaining queue positions
```

### Message Exchange

```
1. User sends message
2. System routes to consultant (if in consultant mode)
3. Consultant sends response
4. System sends to user via WebSocket
5. Message saved as CONSULTANT type
```

### Session Lifecycle

```
AI ACTIVE 
  → WAITING (if consultant not available)
  → ACTIVE (consultant mode, when picked)
  → CLOSED (either party closes)
```

---

## Redis Implementation

**Queue Management:**
- Key: `"chatbot:waiting:queue"` (LIST, FIFO)
- Operations: rightPush (enqueue), leftPop (dequeue)
- Position tracking: `"chatbot:session:position:{sessionId}"` (STRING)

**Performance:**
- O(1) enqueue/dequeue operations
- No database queries for queue management
- Automatic cleanup of position keys

---

## Security Features

1. **Authentication:**
   - JWT token validation in WebSocket headers
   - Principal extraction with type safety
   - Role-based consultant identification

2. **Authorization:**
   - User can only access own sessions
   - Consultant can only access assigned sessions
   - Validation before every operation

3. **Error Handling:**
   - Specific error codes for different scenarios
   - Consistent HTTP status codes
   - Detailed error messages

---

## Performance Optimizations

1. **Redis for Queue:** O(1) operations instead of DB queries
2. **Lazy Loading:** Relationships use FetchType.LAZY
3. **Rate Limiting:** In-memory Guava RateLimiter
4. **Message Batching:** Only load 10 recent messages
5. **DB Indexes:** On (session_id, sent_at)

---

## Testing Insights

**Key Test Scenarios:**
1. Keyword detection with various Korean/English inputs
2. Rate limit enforcement at global and per-user levels
3. Queue FIFO ordering with multiple sessions
4. Session state transitions
5. Message routing between user and consultant
6. Authorization checks
7. Position updates as queue shrinks
8. Invalid session handling in queue

**Critical Assertions:**
- Session mode changes: AI → CONSULTANT
- Session status: ACTIVE → WAITING → ACTIVE → CLOSED
- Message type consistency
- Position numbers decrease monotonically
- Only assigned consultant receives messages

---

## File Locations

### Source Files
- Controllers: `/src/main/java/com/dentallink/domain/chatbot/controller/`
- Services: `/src/main/java/com/dentallink/domain/chatbot/service/`
- Entities: `/src/main/java/com/dentallink/domain/chatbot/entity/`
- Repositories: `/src/main/java/com/dentallink/domain/chatbot/repository/`
- DTOs: `/src/main/java/com/dentallink/domain/chatbot/dto/`
- Enums: `/src/main/java/com/dentallink/domain/chatbot/enums/`
- Exceptions: `/src/main/java/com/dentallink/domain/chatbot/exception/`

### Documentation
- This summary: `EXPLORATION_SUMMARY.md`
- Full analysis: `CONSULTANT_CONNECTION_ANALYSIS.md`
- Code snippets: `CODE_SNIPPETS_REFERENCE.md`
- Quick reference: `CONSULTANT_CONNECTION_QUICK_REFERENCE.md`

---

## How to Use These Documents

### For Quick Understanding
1. Start with `CONSULTANT_CONNECTION_QUICK_REFERENCE.md` (9 KB, 5 min read)
2. Review the flow diagram and state transitions
3. Check the error codes and endpoints tables

### For Implementation
1. Read the relevant section in `CODE_SNIPPETS_REFERENCE.md`
2. Copy snippets and adapt to your needs
3. Cross-reference with `CONSULTANT_CONNECTION_ANALYSIS.md` for context

### For Deep Dive
1. Start with `CONSULTANT_CONNECTION_ANALYSIS.md` (24 KB, 20 min read)
2. Study the detailed flows and examples
3. Review the transaction and consistency handling sections
4. Check security and error handling sections

### For Code Review
1. Use `CODE_SNIPPETS_REFERENCE.md` for line-by-line understanding
2. Reference method signatures and error handling
3. Check authentication and authorization patterns

---

## Key Metrics

- **Files analyzed:** 19 Java files in chatbot domain
- **Code sections covered:** 50+ methods and classes
- **Error types documented:** 13 ChatbotErrorCode enums
- **Keywords detected:** 32+ consultant request keywords
- **WebSocket endpoints:** 6 message mappings
- **REST endpoints:** 3 API endpoints
- **Rate limits:** 2-tier (global + per-user)
- **Database queries:** 7+ custom repository methods
- **State transitions:** 4-state session lifecycle

---

## Quick Access Reference

| Need | File | Section |
|------|------|---------|
| Flow overview | QUICK_REFERENCE | "1 Minute Summary" |
| All error codes | QUICK_REFERENCE | "Error Codes Table" |
| Copy-paste code | CODE_SNIPPETS | Any section |
| Detailed explanation | ANALYSIS | Any section |
| WebSocket endpoints | QUICK_REFERENCE | "WebSocket Endpoints" |
| REST endpoints | QUICK_REFERENCE | "REST API Endpoints" |
| Database queries | QUICK_REFERENCE | "Database Queries" |
| Testing checklist | QUICK_REFERENCE | "Testing Notes" |
| Troubleshooting | QUICK_REFERENCE | "Common Issues" |

---

## Next Steps

These documents provide complete understanding of:
- Current consultant connection implementation
- API contracts and message formats
- Error scenarios and handling
- Performance characteristics
- Security considerations

Use them as reference for:
- Frontend implementation (WebSocket client)
- New feature development (extend consultant system)
- Bug fixes (understand root causes)
- Performance optimization (identify bottlenecks)
- Testing (comprehensive test cases)
- Documentation (API docs for clients)

