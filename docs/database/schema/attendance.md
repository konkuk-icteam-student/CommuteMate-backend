# 출근 기록 시스템 (Attendance System)

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

출근 기록 시스템은 사용자의 실제 출근/퇴근 시간을 기록하고 관리합니다. 근무 일정과 연계하여 실제 근무 현황을 추적합니다.

### 주요 특징
- **출입 기록**: 출근/퇴근 시간 자동 기록
- **인증 상태 추적**: `verified` 필드로 검증 상태 기록 (현재 출퇴근 처리 시 `true`로 저장)
- **근무 일정 연계**: WorkSchedule과 1:N 관계로 일정별 기록 관리
- **인덱싱**: 사용자별, 날짜별 빠른 조회 지원
- **감시 필드**: 생성/수정 시간 및 담당자 자동 기록

### 엔티티 위치
```
src/main/java/com/better/CommuteMate/domain/workattendance/entity/
├── WorkAttendance.java
└── repository/
    └── WorkAttendanceRepository.java
```

---

## 🗂️ 테이블 구조

### work_attendance 테이블

**목적**: 사용자의 실제 출근/퇴근 시간 기록

```sql
CREATE TABLE work_attendance (
    attendance_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    schedule_id INT NOT NULL,
    check_time DATETIME NOT NULL COMMENT '출근/퇴근 시간',
    check_type_code CHAR(4) NOT NULL COMMENT 'CT01: 출근(CHECK_IN), CT02: 퇴근(CHECK_OUT)',
    verified TINYINT(1) NOT NULL DEFAULT FALSE COMMENT '관리자 검증 여부',
    created_at DATETIME NOT NULL,
    created_by INT,
    updated_at DATETIME NOT NULL,
    updated_by INT,

    PRIMARY KEY (attendance_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES work_schedule(schedule_id) ON DELETE CASCADE,
    INDEX idx_wa_user_time (user_id, check_time),
    INDEX idx_wa_schedule (schedule_id)
);
```

---

## 📋 필드 설명

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **attendance_id** | INT | NO | AUTO_INCREMENT | 출근 기록 ID (Primary Key) |
| **user_id** | INT | NO | - | 사용자 ID (Foreign Key → user.user_id) |
| **schedule_id** | INT | NO | - | 근무 일정 ID (Foreign Key → work_schedule.schedule_id) |
| **check_time** | DATETIME | NO | - | 출근/퇴근 시간 |
| **check_type_code** | CHAR(4) | NO | - | 출입 유형 코드 (CT01: 출근, CT02: 퇴근) |
| **verified** | TINYINT(1) | NO | FALSE | 관리자 검증 여부 (FALSE: 미검증, TRUE: 검증됨) |
| **created_at** | DATETIME | NO | - | 생성 시간 (@PrePersist에서 자동 설정) |
| **created_by** | INT | YES | NULL | 생성자 ID |
| **updated_at** | DATETIME | NO | - | 수정 시간 (@PreUpdate에서 자동 설정) |
| **updated_by** | INT | YES | NULL | 수정자 ID |

---

## 🔗 관계

### ERD (Entity Relationship Diagram)
```
┌─────────────────┐
│      User       │
├─────────────────┤
│ user_id (PK)    │
└────────┬────────┘
         │ (1:N)
         │
    ┌────┴──────────────────┐
    │ WorkSchedule (1:N)    │
    ├──────────────────────┤
    │ schedule_id (PK)     │
    │ user_id (FK)         │
    │ start_time/end_time  │
    └────┬───────────────────┘
         │ (1:N)
    ┌────┴──────────────────┐
    │ WorkAttendance       │
    ├──────────────────────┤
    │ attendance_id (PK)   │
    │ user_id (FK)         │
    │ schedule_id (FK)     │
    │ check_time           │
    │ check_type_code      │
    │ verified             │
    └──────────────────────┘
```

### 관계 상세

| 관계 | 설명 | 타입 | 비고 |
|------|------|------|------|
| User → WorkAttendance | 사용자가 여러 출근 기록 | 1:N | user_id FK, cascade delete |
| WorkSchedule → WorkAttendance | 일정에 대한 출근 기록 | 1:N | schedule_id FK, cascade delete |

---

## 🔢 CodeType 연동

### WorkAttendance.checkTypeCode (출입 유형)

**CodeType Enum**: `CT` (출입 유형)

| 코드 | 값 | 한글명 | 영문명 | 설명 |
|------|-----|--------|--------|------|
| **CT01** | `CHECK_IN` | 출근 | Check In | 사용자가 출근 시간을 기록 |
| **CT02** | `CHECK_OUT` | 퇴근 | Check Out | 사용자가 퇴근 시간을 기록 |

### 사용 방식

```java
// Java에서 checkTypeCode 사용
attendance.setCheckTypeCode(CodeType.CT01);  // 출근

// 출입 유형 조회
if (attendance.getCheckTypeCode().equals(CodeType.CT01)) {
    // 출근 기록 처리
}

// 전체 코드 값 조회
String fullCode = CodeType.CT01.getFullCode();  // "CT01"
CodeType code = CodeType.fromFullCode("CT01");  // CodeType.CT01
```

---

## 💻 사용 예시

### 1. 출근 기록 생성

```java
// 출근 시간 기록
WorkAttendance checkIn = WorkAttendance.builder()
    .user(user)
    .schedule(workSchedule)
    .checkTime(LocalDateTime.of(2025, 12, 15, 09, 15))
    .checkTypeCode(CodeType.CT01)  // 출근
    .verified(true)  // 출퇴근 처리 시 true로 저장
    .createdBy(userId)
    .build();

workAttendanceRepository.save(checkIn);

// 퇴근 시간 기록
WorkAttendance checkOut = WorkAttendance.builder()
    .user(user)
    .schedule(workSchedule)
    .checkTime(LocalDateTime.of(2025, 12, 15, 18, 30))
    .checkTypeCode(CodeType.CT02)  // 퇴근
    .verified(true)
    .createdBy(userId)
    .build();

workAttendanceRepository.save(checkOut);
```

### 2. 사용자별 출근 기록 조회

```java
// 특정 사용자와 시간으로 출근 기록 조회
LocalDateTime checkTime = LocalDateTime.of(2025, 12, 15, 09, 15);
List<WorkAttendance> records = workAttendanceRepository
    .findByUser_UserIdAndCheckTime(userId, checkTime);

// 특정 날짜 범위의 출근 기록 조회
LocalDateTime start = LocalDateTime.of(2025, 12, 15, 0, 0);
LocalDateTime end = LocalDateTime.of(2025, 12, 15, 23, 59);

List<WorkAttendance> dayRecords = workAttendanceRepository
    .findByUser_UserIdAndCheckTimeBetween(userId, start, end);

// 특정 일정의 출근/퇴근 기록
List<WorkAttendance> scheduleRecords = workAttendanceRepository
    .findBySchedule_ScheduleId(scheduleId);
```

### 3. 기록 검증

```java
// 관리자가 출근 기록 검증
WorkAttendance record = workAttendanceRepository.findById(attendanceId).orElse(null);

if (record != null) {
    record.setVerified(true);  // 검증 완료
    record.setUpdatedBy(adminId);
    workAttendanceRepository.save(record);
}
```

### 4. 일일 근무 현황 조회

```java
// 특정 날짜의 사용자별 출입 기록 조회
LocalDateTime startOfDay = LocalDateTime.of(2025, 12, 15, 0, 0);
LocalDateTime endOfDay = LocalDateTime.of(2025, 12, 15, 23, 59);

List<WorkAttendance> dayAttendances = workAttendanceRepository
    .findByUser_UserIdAndCheckTimeBetween(userId, startOfDay, endOfDay);

// 출근/퇴근 시간 매핑
LocalDateTime checkInTime = null;
LocalDateTime checkOutTime = null;

for (WorkAttendance attendance : dayAttendances) {
    if (attendance.getCheckTypeCode().equals(CodeType.CT01)) {
        checkInTime = attendance.getCheckTime();  // 출근 시간
    } else if (attendance.getCheckTypeCode().equals(CodeType.CT02)) {
        checkOutTime = attendance.getCheckTime();  // 퇴근 시간
    }
}
```

---

## ⚠️ 주의사항

### 1. 검증 상태 관리
- `verified` 필드는 검증 여부를 나타냄
- 기본값은 **FALSE**이지만, 현재 출퇴근 처리 로직에서 `true`로 저장됨
- 별도 수동 검증 로직은 구현되어 있지 않음

```java
// ✅ 현재 로직: 출퇴근 시 verified = true로 저장
WorkAttendance attendance = WorkAttendance.builder()
    .verified(true)
    .build();
```

### 2. 시간대 처리
- 모든 시간 필드는 **LocalDateTime** 사용
- 출근/퇴근 시간은 실제 기록된 시간 (정확한 시간 초 단위)
- 근무 일정의 시간과 출입 시간은 서로 다를 수 있음

```java
// ✅ 올바른 예
WorkSchedule schedule;  // 09:00 ~ 18:00
WorkAttendance checkIn;  // 09:15 (실제 출근 시간)
WorkAttendance checkOut;  // 18:30 (실제 퇴근 시간)

// 시간 검증 로직
if (checkIn.getCheckTime().isBefore(schedule.getStartTime())) {
    // 조기 출근
}
```

### 3. 사용자 및 일정 FK 제약
- `user_id`와 `schedule_id` 모두 필수 필드
- 부모 테이블에서 삭제 시 자동 삭제 (CASCADE)
- 고아 레코드(orphan record) 방지

```java
// ❌ 잘못된 예 - 필수 필드 누락
WorkAttendance attendance = WorkAttendance.builder()
    .checkTime(LocalDateTime.now())
    .checkTypeCode(CodeType.CT01)
    .build();  // user, schedule 누락

// ✅ 올바른 예
WorkAttendance attendance = WorkAttendance.builder()
    .user(user)
    .schedule(workSchedule)
    .checkTime(LocalDateTime.now())
    .checkTypeCode(CodeType.CT01)
    .build();
```

### 4. 중복 기록 방지
- 동일 사용자의 동일 일정에 **중복 출근/퇴근 기록** 방지 로직 필요
- 데이터베이스 레벨 유니크 제약은 없으므로 **애플리케이션 레벨 검증** 필수

```java
// ✅ 권장: 출근/퇴근 기록 조회 후 중복 확인
List<WorkAttendance> existingCheckIn = workAttendanceRepository
    .findByUserAndScheduleAndCheckTypeCode(user, schedule, CodeType.CT01);

if (!existingCheckIn.isEmpty()) {
    throw new DuplicateCheckInException("이미 출근 기록이 있습니다.");
}
```

### 5. 인덱싱 최적화
- `idx_wa_user_time` 인덱스: 사용자별 시간 범위 조회 최적화
- `idx_wa_schedule` 인덱스: 특정 일정의 모든 출입 기록 조회 최적화

```java
// 인덱스 활용하는 쿼리
// ✅ idx_wa_user_time 인덱스 사용
List<WorkAttendance> records =
    workAttendanceRepository.findByUserAndCheckTimeBetween(user, startTime, endTime);

// ✅ idx_wa_schedule 인덱스 사용
List<WorkAttendance> scheduleAttendances =
    workAttendanceRepository.findBySchedule(schedule);
```

### 6. 감시 필드 자동 설정
- `created_at`, `created_by`: 생성 시 자동 설정 (@PrePersist)
- `updated_at`, `updated_by`: 수정 시마다 자동 갱신 (@PreUpdate)

```java
// ❌ 잘못된 예
attendance.setCreatedAt(LocalDateTime.now());  // 직접 설정 금지

// ✅ 올바른 예
attendance.setUpdatedBy(adminId);  // 수정자 ID만 지정
// created_at, updated_at은 자동 설정됨
```

---

## 🔗 관련 문서

- **docs/database/schema/user.md** - 사용자(User) 엔티티 상세
- **docs/database/schema/schedule.md** - 근무 일정 엔티티 상세
- **docs/database/schema/task.md** - 업무(Task) 시스템 상세
- **docs/api/endpoints-summary.md** - 출근 기록 API 상세 로직
- **docs/conventions/error-handling.md** - 에러/예외 처리 규칙
- **CLAUDE.md** - 프로젝트 구조 및 기술 스택
