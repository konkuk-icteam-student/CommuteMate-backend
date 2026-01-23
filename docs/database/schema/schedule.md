# 근무 일정 시스템 (Schedule System)

## 📑 목차
- [개요](#개요)
- [테이블 구조](#테이블-구조)
- [필드 설명](#필드-설명)
- [관계](#관계)
- [CodeType 연동](#codetype-연동)
- [사용 예시](#사용-예시)
- [주의사항](#주의사항)
- [관련 문서](#관련-문서)

---

## 📖 개요

근무 일정 시스템은 사용자의 근무 일정 신청, 승인, 관리를 담당합니다. 월별 동시 근무 인원 제한, 신청 기간 관리 등의 기능을 제공합니다.

### 주요 특징
- **계층적 상태 관리**: 신청 → 승인/거부 → 취소 상태 추적
- **월별 제한 관리**: 동시 근무 인원수 제한 및 신청 가능 기간 설정
- **소프트 삭제**: 일정 삭제 시 기록 보존을 위해 `isDeleted` 플래그 사용
- **감시 필드**: 생성/수정 시간 및 담당자 자동 기록
- **관련 기록 추적**: WorkAttendance, WorkChangeRequest와의 연관 관리

### 엔티티 위치
```
src/main/java/com/better/CommuteMate/domain/schedule/entity/
├── WorkSchedule.java
├── MonthlyScheduleConfig.java
└── repository/
    ├── WorkSchedulesRepository.java
    └── MonthlyScheduleConfigRepository.java
```

---

## 🗂️ 테이블 구조

### 1. work_schedule 테이블

**목적**: 사용자의 근무 일정 신청 정보 저장

```sql
CREATE TABLE work_schedule (
    schedule_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status_code CHAR(4) NOT NULL COMMENT 'WS01: 신청됨, WS02: 승인됨, WS03: 거부됨, WS04: 취소됨',
    is_deleted TINYINT(1) NOT NULL DEFAULT FALSE COMMENT '소프트 삭제 플래그',
    created_at DATETIME NOT NULL,
    created_by INT NOT NULL,
    updated_at DATETIME NOT NULL,
    updated_by INT,

    PRIMARY KEY (schedule_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_ws_user (user_id),
    INDEX idx_ws_status (status_code)
);
```

### 2. monthly_schedule_config 테이블

**목적**: 월별 근무 일정 제한 및 신청 기간 설정

```sql
CREATE TABLE monthly_schedule_config (
    limit_id INT NOT NULL AUTO_INCREMENT,
    schedule_year INT NOT NULL COMMENT '신청 대상 연도',
    schedule_month INT NOT NULL COMMENT '신청 대상 월',
    max_concurrent INT NOT NULL COMMENT '동시 근무 최대 인원수',
    apply_start_time DATETIME NOT NULL COMMENT '신청 시작 시간',
    apply_end_time DATETIME NOT NULL COMMENT '신청 종료 시간',
    created_at DATETIME NOT NULL,
    created_by INT NOT NULL,
    updated_at DATETIME,
    updated_by INT,

    PRIMARY KEY (limit_id),
    UNIQUE INDEX unique_month (schedule_year, schedule_month),
    INDEX idx_month (schedule_year, schedule_month)
);
```

### 3. work_change_request 테이블

**목적**: 근무 일정 변경/삭제 요청 기록

```sql
CREATE TABLE work_change_request (
    request_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    schedule_id INT NOT NULL,
    type_code CHAR(4) NOT NULL COMMENT 'CR01: 수정, CR02: 삭제',
    reason TEXT,
    status_code CHAR(4) NOT NULL COMMENT 'CS01: 대기, CS02: 승인, CS03: 거부',
    created_at DATETIME NOT NULL,
    created_by INT NOT NULL,
    updated_at DATETIME NOT NULL,
    updated_by INT,

    PRIMARY KEY (request_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (schedule_id) REFERENCES work_schedule(schedule_id),
    INDEX idx_wcr_user (user_id),
    INDEX idx_wcr_schedule (schedule_id)
);
```

---

## 📋 필드 설명

### work_schedule 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **schedule_id** | INT | NO | AUTO_INCREMENT | 근무 일정 ID (Primary Key) |
| **user_id** | INT | NO | - | 사용자 ID (Foreign Key → user.user_id) |
| **start_time** | DATETIME | NO | - | 근무 시작 시간 |
| **end_time** | DATETIME | NO | - | 근무 종료 시간 |
| **status_code** | CHAR(4) | NO | - | 상태 코드 (WS01/WS02/WS03/WS04) |
| **is_deleted** | TINYINT(1) | NO | FALSE | 소프트 삭제 플래그 (TRUE: 삭제됨) |
| **created_at** | DATETIME | NO | - | 생성 시간 (@PrePersist에서 자동 설정) |
| **created_by** | INT | NO | - | 생성자 ID (로그인한 사용자 ID) |
| **updated_at** | DATETIME | NO | - | 수정 시간 (@PreUpdate에서 자동 설정) |
| **updated_by** | INT | YES | NULL | 수정자 ID |

### monthly_schedule_config 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **limit_id** | INT | NO | AUTO_INCREMENT | 월별 제한 ID (Primary Key) |
| **schedule_year** | INT | NO | - | 신청 대상 연도 (예: 2025) |
| **schedule_month** | INT | NO | - | 신청 대상 월 (1-12) |
| **max_concurrent** | INT | NO | - | 동시 근무 최대 인원수 (서비스에서 기본값 적용) |
| **apply_start_time** | DATETIME | NO | - | 신청 시작 시간 |
| **apply_end_time** | DATETIME | NO | - | 신청 종료 시간 |
| **created_at** | DATETIME | NO | - | 생성 시간 |
| **created_by** | INT | NO | - | 생성자 ID |
| **updated_at** | DATETIME | NO | - | 수정 시간 |
| **updated_by** | INT | YES | NULL | 수정자 ID |

### work_change_request 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **request_id** | INT | NO | AUTO_INCREMENT | 변경 요청 ID |
| **user_id** | INT | NO | - | 요청한 사용자 ID |
| **schedule_id** | INT | NO | - | 대상 일정 ID |
| **type_code** | CHAR(4) | NO | - | 요청 유형 (CR01/CR02) |
| **reason** | TEXT | YES | NULL | 요청 사유 |
| **status_code** | CHAR(4) | NO | - | 처리 상태 (CS01/CS02/CS03) |
| **created_at** | DATETIME | NO | - | 생성 시간 |
| **created_by** | INT | NO | - | 생성자 ID |
| **updated_at** | DATETIME | NO | - | 수정 시간 |
| **updated_by** | INT | YES | NULL | 수정자 ID |

---

## 🔗 관계

### ERD (Entity Relationship Diagram)
```
┌─────────────────┐
│      User       │
├─────────────────┤
│ user_id (PK)    │
│ email           │
│ roleCode        │
└────────┬────────┘
         │ (1:N)
         │
    ┌────┴──────────────────┐
    │ WorkSchedule (1:N)    │
    ├──────────────────────┤
    │ schedule_id (PK)     │
    │ user_id (FK)         │
    │ start_time/end_time  │
    │ status_code          │
    │ is_deleted           │
    └────┬───────────────────┘
         │ (1:N)
    ┌────┴──────────────────┐
    │ WorkAttendance       │
    ├──────────────────────┤
    │ attendance_id (PK)   │
    │ schedule_id (FK)     │
    │ checkType            │
    └──────────────────────┘

    ┌──────────────────────┐
    │ WorkChangeRequest   │
    ├──────────────────────┤
    │ request_id (PK)      │
    │ schedule_id (FK)     │
    │ typeCode (CR01/02)   │
    │ statusCode (CS01/02) │
    └──────────────────────┘

    ┌──────────────────────┐
    │ MonthlyScheduleConfig│
    ├──────────────────────┤
    │ limit_id (PK)        │
    │ schedule_year/month  │
    │ maxConcurrent        │
    │ applyStartTime/End   │
    └──────────────────────┘
```

### 관계 상세

| 관계 | 설명 | 타입 | 비고 |
|------|------|------|------|
| User → WorkSchedule | 사용자가 여러 근무 일정 신청 | 1:N | user_id FK, cascade delete |
| WorkSchedule → WorkAttendance | 일정에 대한 출근 기록 | 1:N | schedule_id FK |
| WorkSchedule → WorkChangeRequest | 일정 변경 요청 | 1:N | schedule_id FK |
| (없음) | MonthlyScheduleConfig는 독립적 설정 | - | 년월 기준 유니크 |

---

## 🔢 CodeType 연동

### WorkSchedule.statusCode (근무 일정 상태)

**CodeType Enum**: `WS` (근무 상태)

| 코드 | 값 | 한글명 | 영문명 | 설명 |
|------|-----|--------|--------|------|
| **WS01** | `REQUESTED` | 신청됨 | Requested | 사용자가 근무 일정 신청 직후 초기 상태 |
| **WS02** | `APPROVED` | 승인됨 | Approved | 관리자가 신청한 일정을 승인한 상태 |
| **WS03** | `REJECTED` | 거부됨 | Rejected | 관리자가 신청한 일정을 거부한 상태 |
| **WS04** | `CANCELLED` | 취소됨 | Cancelled | 사용자 또는 관리자가 승인된 일정을 취소한 상태 |

### 상태 전이 흐름

```
신청됨 (WS01)
    ├─→ 승인됨 (WS02) ─→ 취소됨 (WS04)
    └─→ 거부됨 (WS03)
```

### 사용 방식

```java
// Java에서 statusCode 사용
workSchedule.setStatusCode(CodeType.WS01);  // 신청됨

// 상태 조회
if (workSchedule.getStatusCode().equals(CodeType.WS02)) {
    // 승인된 일정만 처리
}

// 전체 코드 값 조회
String fullCode = CodeType.WS01.getFullCode();  // "WS01"
CodeType code = CodeType.fromFullCode("WS01");  // CodeType.WS01
```

---

## 💻 사용 예시

### 1. 근무 일정 신청

```java
// 신규 일정 생성
WorkSchedule schedule = WorkSchedule.builder()
    .user(user)
    .startTime(LocalDateTime.of(2025, 12, 15, 09, 0))
    .endTime(LocalDateTime.of(2025, 12, 15, 18, 0))
    .statusCode(CodeType.WS01)  // 신청됨
    .createdBy(userId)
    .build();

workSchedulesRepository.save(schedule);
```

### 2. 근무 일정 조회

```java
// 특정 기간의 일정 조회 (유효한 일정만)
List<WorkSchedule> schedules = workSchedulesRepository
    .findValidSchedulesByUserAndDateRange(userId, startOfDay, endOfDay);

// 특정 기간의 전체 일정 조회 (삭제된 일정 제외)
List<WorkSchedule> schedules = workSchedulesRepository
    .findAllSchedulesByUserAndDateRange(userId, startOfDay, endOfDay);

// 특정 사용자와 시작 시간으로 일정 조회
Optional<WorkSchedule> schedule = workSchedulesRepository
    .findByUserAndStartTime(user, startTime);
```

### 3. 근무 일정 승인/거부

```java
// 일정 승인
schedule.setStatusCode(CodeType.WS02);
schedule.setUpdatedBy(adminId);
workSchedulesRepository.save(schedule);

// 일정 거부
schedule.setStatusCode(CodeType.WS03);
schedule.setUpdatedBy(adminId);
workSchedulesRepository.save(schedule);
```

### 4. 근무 일정 삭제 (소프트 삭제)

```java
// 이전 메서드 유지 (호환성)
schedule.deleteApplySchedule();
workSchedulesRepository.save(schedule);

// 또는 직접 설정
schedule.setIsDeleted(true);
schedule.setUpdatedBy(userId);
workSchedulesRepository.save(schedule);
```

### 5. 월별 제한 설정

```java
// 2025년 12월 최대 10명 동시 근무 설정
LocalDateTime applyStart = LocalDateTime.of(2025, 11, 23, 0, 0);
LocalDateTime applyEnd = LocalDateTime.of(2025, 11, 27, 0, 0);

MonthlyScheduleConfig config = MonthlyScheduleConfig.builder()
    .scheduleYear(2025)
    .scheduleMonth(12)
    .maxConcurrent(10)
    .applyStartTime(applyStart)
    .applyEndTime(applyEnd)
    .createdBy(adminId)
    .build();

monthlyScheduleConfigRepository.save(config);
```

### 6. 월별 제한 조회

```java
// 특정 년월의 제한 조회
Optional<MonthlyScheduleConfig> config =
    monthlyScheduleConfigRepository
    .findByScheduleYearAndScheduleMonth(2025, 12);

int maxConcurrent = config
    .map(MonthlyScheduleConfig::getMaxConcurrent)
    .orElse(10);  // 기본값 10

// 신청 기간 확인
LocalDateTime now = LocalDateTime.now();
boolean canApply = now.isAfter(config.getApplyStartTime())
                && now.isBefore(config.getApplyEndTime());
```

---

## ⚠️ 주의사항

### 1. 소프트 삭제 처리
- `work_schedule` 테이블의 일정 삭제는 `isDeleted = TRUE`로 표시되어 **물리적 삭제가 아님**
- 쿼리 작성 시 반드시 `is_deleted = FALSE` 조건 추가
- 예: 기간 기반 조회 메서드 사용 권장

```java
// ✅ 올바른 예 - 미삭제 일정만 조회
LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
LocalDateTime end = start.plusMonths(1);
List<WorkSchedule> schedules =
    workSchedulesRepository.findAllSchedulesByUserAndDateRange(userId, start, end);
```

### 2. 상태 코드 검증
- 상태 전이는 **특정 경로만 허용**
- 임의의 상태 전이는 비즈니스 규칙 위반
- 상태 변경 전에 `ScheduleValidator`로 검증

```java
// 상태 전이 검증
if (!schedule.getStatusCode().equals(CodeType.WS02)) {
    throw new InvalidStateTransitionException();
}

// WS02 (승인됨)에서만 취소 가능
schedule.setStatusCode(CodeType.WS04);
```

### 3. 월별 제한 기본값
- `maxConcurrent` 기본값: **10명**
- 신청 기간이 설정되지 않은 경우 자동 계산:
  - 신청 시작: 해당 월의 전달 23일 00:00
  - 신청 종료: 해당 월의 전달 27일 00:00

```java
// 예: 2025년 12월 제한 설정 시
// 신청 기간 자동 계산
// apply_start_time = 2025-11-23 00:00:00
// apply_end_time = 2025-11-27 00:00:00
```

### 4. 시간대 처리
- 모든 시간 필드는 **LocalDateTime** 사용
- 데이터베이스에 저장될 때 서버 타임존 기준으로 저장
- 조회 시 반드시 타임존 고려

```java
// ✅ 올바른 예
LocalDateTime start = LocalDateTime.of(2025, 12, 15, 09, 0);

// ❌ 피해야 할 예
LocalDateTime start = LocalDateTime.now();  // 부정확한 날짜
```

### 5. 중복/동시 근무 검증
- 중복 일정 및 동시 근무 인원 검증은 `ScheduleValidator`에서 수행
- DB 레벨 `schedule_date` 유니크 제약은 없음

```java
// 동시 근무 인원 검증 예시
if (!scheduleValidator.isScheduleInsertable(slot)) {
    // 해당 슬롯은 실패 목록에 포함
}
```

### 6. 감시 필드 자동 설정
- `created_at`, `created_by`: 생성 시 자동 설정 (@PrePersist)
- `updated_at`, `updated_by`: 수정 시마다 자동 갱신 (@PreUpdate)
- 명시적으로 수정하면 안 됨

```java
// ❌ 잘못된 예 - 자동 설정되는 필드 직접 수정
schedule.setCreatedAt(LocalDateTime.now());

// ✅ 올바른 예 - 비즈니스 필드만 수정
schedule.setStatusCode(CodeType.WS02);
schedule.setUpdatedBy(adminId);  // 수정자 ID만 지정
```

---

## 🔗 관련 문서

- **docs/database/schema/user.md** - 사용자(User) 엔티티 상세
- **docs/database/schema/faq.md** - FAQ 시스템 상세
- **docs/api/endpoints-summary.md** - 근무 일정 API 상세 로직
- **docs/conventions/error-handling.md** - 에러/예외 처리 규칙
- **docs/database/schema/code-system.md** - CodeType Enum 전체 시스템
- **CLAUDE.md** - 프로젝트 구조 및 기술 스택
