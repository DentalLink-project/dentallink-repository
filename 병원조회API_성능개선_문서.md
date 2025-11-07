# 📊 Reservation 조회 API 성능 개선 보고서

> **프로젝트:** 치과 예약 및 결제 시스템 (DentalLink)  
> **개선 날짜:** 2025-11-05  
> **개선 대상:** 병원 전체 조회 및 병원 상세 조회 API

---

## 🎯 개선 목표
1. 병원 전체 조회
- 병원 전체 조회 API의 **N+1 문제**와 **중복 쿼리 비효율성**을 제거하여
- 로그인한 사용자의 즐겨찾기 여부를 함께 조회하면서도 **응답 속도를 개선**한다.

2. 병원 단건 조회
- 병원 상세 조회 API에서 로그인한 사용자의 즐겨찾기(favorite) 여부를 함께 보여주면서도, 병원(Hospital) + 스케줄(HospitalSchedule) + 의사(Doctor) 정보를 한 번의 쿼리로 조회하도록 개선한다.

---

## 🔍 문제점 분석

### 1. 기존 문제 상황
1. 병원 전체 조회
- 병원 전체 조회 시, 로그인한 사용자의 **즐겨찾기(favorite)** 여부를 확인하기 위해 각 병원마다 별도의 쿼리를 수행 → **N+1 문제 발생**
- `hospital` 테이블과 `favorite` 테이블을 **별도 쿼리**로 조회하여 데이터베이스 부하 및 응답 지연이 발생

2. 병원 단건 조회
- 병원 상세 정보를 조회할 때 병원(hospital)과 스케줄(hospital_schedule)을 각각 조회
- 로그인한 사용자의 즐겨찾기 여부를 확인하기 위해 favorite 테이블을 추가로 조회
- 병원 상세 1건 조회에도 3~4개의 쿼리가 실행되어 응답 지연 발생



### 2. 발견된 문제점

#### 문제 API: 병원 전체 조회 (`GET /api/hospitals`)
- **문제:** 각 병원마다 Favorite 테이블을 개별 조회 (N+1 문제)
- **쿼리 횟수:** 병원 10개 기준 약 11회 쿼리 발생
- **응답 시간:** 약 320ms

### 🔧 개선 전 코드

```java
// 병원 전체 조쇠
@Transactional(readOnly = true)
public PageResponse<HospitalListResponse> findAllHospitals(int page, int size) {
    Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
    Page<Hospital> hospitals = hospitalRepository.findAll(pageable);
    Page<HospitalListResponse> hospitalResponse = hospitals.map(HospitalListResponse::from);
    return PageResponse.fromPage(hospitalResponse);
}

// 병원 단건 조회
public ResponseEntity<CommonApiResponse<HospitalDetailResponse>> getHospitalById(@PathVariable Long id) {
    HospitalDetailResponse hospital = hospitalInternalService.findHospitalById(id);
    return success(hospital, "병원 상세 정보를 조회했습니다.");
}
```

### ✅ 개선 방법

### 해결책: Fetch Join 사용

- JPA의 **Fetch Join**과 **JPQL DTO Projection**을 활용하여 병원 정보와 즐겨찾기 여부를 **한 번의 쿼리로 조회**
- 로그인하지 않은 사용자는 Favorite 조인 없이 조회 가능하도록 설계
- DTO Projection으로 필요한 필드만 선택 → 응답 크기 최소화
- 로그인하지 않은 사용자는 userId = null 처리

#### Controller 수정
```java
// HospitalController.java
// 병원 전체 조회
public ResponseEntity<CommonApiResponse<PageResponse<HospitalListResponse>>> getAllHospitals(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal AuthUser authUser
) {
    Long userId = (authUser != null) ? authUser.getUserId() : null;

    return success(
            hospitalInternalService.findAllHospitals(page, size, userId),
            "병원 목록을 조회했습니다."
    );
}

// 병원 단건 조회
public ResponseEntity<CommonApiResponse<HospitalDetailResponse>> getHospitalById(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthUser authUser
) {
    Long userId = (authUser != null) ? authUser.getUserId() : null;
    HospitalDetailResponse hospital = hospitalInternalService.findHospitalById(id, userId);
    return success(hospital, "병원 상세 정보를 조회했습니다.");
}
```

#### Service 수정

```java
// HospitalInternalService.java
// 병원 전체 조회
public PageResponse<HospitalListResponse> findAllHospitals(int page, int size, Long userId) {
    Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
    Page<HospitalListResponse> hospitals = hospitalRepository.findAllWithOptionalFavorite(userId, pageable);
    return PageResponse.fromPage(hospitals);
}

// 병원 단건 조회
public HospitalDetailResponse findHospitalById(Long id, Long userId) {
    return hospitalRepository.findHospitalDetailWithOptionalFavorite(id, userId)
            .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
}
```

#### Repository 수정

```java
// HospitalRepository.java
@Query(value = """
        SELECT new com.dentallink.domain.hospital.dto.response.HospitalListResponse(
                h.id,
                h.hospitalName,
                h.doctorName,
                h.hospitalIsOpen,
                (f.id IS NOT NULL)
            )
            FROM Hospital h
            LEFT JOIN Favorite f
                ON f.hospital = h
                AND f.user.id = :userId
            ORDER BY h.id DESC
        """,
        countQuery = "SELECT COUNT(h) FROM Hospital h")
Page<HospitalListResponse> findAllWithOptionalFavorite(
        @Param("userId") Long userId,
        Pageable pageable
);

@Query("""
            SELECT new com.dentallink.domain.hospital.dto.response.HospitalDetailResponse(
                    h.id,
                    h.hospitalName,
                    h.hospitalDescription,
                    h.hospitalAddress,
                    h.hospitalIsOpen,
                    h.doctorName,
                    h.reservationCost,
                    s.openTime,
                    s.closeTime,
                    s.breakStart,
                    s.breakEnd,
                    (f.id IS NOT NULL)
                )
                FROM Hospital h
                LEFT JOIN h.hospitalSchedule s
                LEFT JOIN Favorite f
                    ON f.hospital = h
                    AND f.user.id = :userId
                WHERE h.id = :hospitalId
        """)
Optional<HospitalDetailResponse> findHospitalDetailWithOptionalFavorite(
        @Param("hospitalId") Long hospitalId,
        @Param("userId") Long userId
);
```

## 📈 성능 측정 결과

### API : 병원 전체 조회(로그인한 사용자 시)
- nGrinder 테스트 환경 : Agent = 1, Process = 10, Thread = 20

| 항목           | Before | After | 개선율      |
|--------------|--------|-------|----------|
| **쿼리 실행 횟수** | 11번    | 1번    | **91% ↓** |
| **응답 시간**    | 320ms  | 120ms | **63% ↓** |
| **TPS**      | 85.1   | 115.8 | **36% ↑** |

#### 쿼리 로그 비교

**Before (N+1 발생):**
```sql
SELECT * FROM hospital ORDER BY id DESC LIMIT 10;      -- 1회
SELECT * FROM favorite WHERE hospital_id = ? AND user_id = ?; -- 10회
-- 총 11회

SELECT * FROM hospital WHERE id = ?;                    -- 1회
SELECT * FROM hospital_schedule WHERE hospital_id = ?;   -- 1회
SELECT * FROM favorite WHERE hospital_id = ? AND user_id = ?; -- 1회
-- 총 3회
```

**After (Fetch Join 적용):**
```sql
SELECT h.id, h.hospital_name, h.doctor_name, h.is_open, (f.id IS NOT NULL)
FROM hospital h
         LEFT JOIN favorite f ON f.hospital_id = h.id AND f.user_id = ?
ORDER BY h.id DESC
    LIMIT 10; -- 단 1회                                             

SELECT h.id, h.hospital_name, h.hospital_description, h.hospital_address,
       s.open_time, s.close_time, s.break_start, s.break_end,
       (f.id IS NOT NULL)
FROM hospital h
         LEFT JOIN hospital_schedule s ON s.hospital_id = h.id
         LEFT JOIN favorite f ON f.hospital_id = h.id AND f.user_id = ?
WHERE h.id = ?; -- 단 1회
```

### API 2: 병원별 예약 목록 조회

| 항목 | Before | After | 개선율 |
|------|--------|-------|--------|
| **쿼리 실행 횟수** | 14번 | 4번 | **71% ↓** |
| **응답 시간** | 150ms | 31ms | **79% ↓** |

#### 쿼리 로그 비교

**Before (N+1 발생):**
```sql
SELECT * FROM reservations WHERE hospital_id = ?;      -- 1번
SELECT * FROM users WHERE id = ?;                       -- 10번 (N+1!)
SELECT * FROM hospital WHERE id = ?;                    -- 1번
SELECT COUNT(*) FROM reservations WHERE hospital_id = ?; -- 1번
-- 총 14번 (추정)
```

**After (Fetch Join):**
```sql
-- Reservation, Hospital, User를 한 번에 조회
SELECT r.*, h.*, u.* 
FROM reservations r 
JOIN hospital h ON r.hospital_id = h.id 
JOIN users u ON r.user_id = u.id 
WHERE r.hospital_id = ? 
LIMIT 10;                                               -- 1번

-- 인증용 User 조회
SELECT * FROM users WHERE id = ?;                       -- 1번

-- Count 쿼리
SELECT COUNT(*) FROM reservations WHERE hospital_id = ?; -- 1번

-- 기타
SELECT ...                                              -- 1번

-- 총 4번!
```
---

## 🎯 전체 개선 효과

### 핵심 지표
1. 병원 전체 조회

✅ **병원 + 즐겨찾기 정보를 1회 쿼리로 처리**  
✅ **응답 속도 평균 200ms 단축**  
✅ **단일 Repository 쿼리에서 모든 데이터 조회 가능**

2. 병원 단건 조회

✅ **병원 + 스케줄 + 즐겨찾기 정보를 1회 쿼리로 처리**  
✅ **응답 속도 평균 120ms 단축**  
✅ **단일 Repository 에서 DTO로 코드 구조 완결**
✅ **즐겨찾기, 리뷰 등 관계 추가 시 구조 유지 가능**

### 그래프로 보는 개선 효과

**쿼리 실행 횟수 비교:**
```
API 1:  ██████████████████ 15회 → ███ 3회 (80% 감소)
API 2:  ███████████████████ 12회 → ███ 3회 (75% 감소)
```

**응답 시간 비교:**
```
API 1:  ██████████████████████ 450ms → ████ 70ms (84% 향상)
API 2:  ██████████████ 210ms → ███ 48ms (77% 향상)
```

---

## 🛠️ 측정 환경 및 방법

### 환경
- **Database:** MySQL 8.0
- **Application:** Spring Boot 3.x + JPA
- **테스트 데이터:** 병원 100개
- **성능 도구:** nGrinder (1 agent, 10 process, 20 thread)

---

## 💡 배운 점 및 개선 방향

### 배운 점
1. Fetch Join + DTO Projection 으로도 충분히 복잡한 데이터 관계를 효율적으로 조회 가능
2. Fetch Join은 로그인 사용자 컨텍스트 기반 조인에도 활용 가능
3. N+1 문제 제거가 곧 TPS 개선과 서버 부하 감소로 이어짐
3. 단순한 쿼리 최적화로도 실사용 성능이 크게 향상될 수 있음
4. 단일 DTO Projection으로 복합 관계 데이터를 한 번에 처리 가능
5. Controller → Service → Repository 간 userId 전달 구조로 유지보수성 향상

### 향후 개선 가능 사항
1. 검색 기능 연계 최적화 – 병원명 검색 시에도 동일한 Fetch Join 적용
2. 캐싱 도입 – 자주 조회되는 병원 목록에 Redis 캐싱 검토
3. DB 인덱스 튜닝 – hospital_id, user_id 복합 인덱스 최적화
---

## 📚 참고 자료

- [JPA N+1 문제와 Fetch Join](https://incheol-jung.gitbook.io/docs/q-and-a/spring/n+1)
- [Spring Data JPA DTO Projection](https://www.baeldung.com/spring-data-jpa-projections)

---


