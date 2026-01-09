# CommuteMate Database Schema

## Overview
CommuteMate 백엔드 시스템의 데이터베이스 스키마 문서입니다.

## ERD
전체 ERD: https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757

---

## 1. Code System (코드 마스터)

### 1.1 `code_major`
코드 대분류 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| major_code | CHAR(2) | PK | 대분류 코드 (예: WS, CR, CS) |

**대분류 코드 목록**:
- **WS**: 근무신청 상태 (Work Schedule Status)
- **CR**: 요청 유형 (Change Request Type)
- **CS**: 요청 상태 (Change Request Status)
- **CT**: 출근 인증 타입 (Check Type)
- **TT**: 업무 유형 (Task Type)
- **RL**: 사용자 역할 (Role)

---

### 1.2 `code_sub`
코드 소분류 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| major_code | CHAR(2) | PK, FK | 대분류 코드 |
| sub_code | CHAR(2) | PK | 소분류 코드 |
| sub_name | VARCHAR(100) | NOT NULL | 소분류 이름 |

**Relationships**:
- `major_code` → `code_major(major_code)`

---

### 1.3 `code`
코드 마스터 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| full_code | CHAR(4) | PK | 전체 코드 (major_code + sub_code) |
| major_code | CHAR(2) | NOT NULL | 대분류 코드 |
| sub_code | CHAR(2) | NOT NULL | 소분류 코드 |
| code_name | VARCHAR(100) | NOT NULL | 코드 이름 |
| code_value | VARCHAR(100) | NOT NULL | 코드 값 |

**Index**:
- `uq_code_major_sub` (UNIQUE): `major_code`, `sub_code`

---

### Code Values (CodeType Enum)

| Code | Name | Value | Description |
|------|------|-------|-------------|
| **WS01** | REQUESTED | 신청 | 근무신청 상태: 신청됨 |
| **WS02** | APPROVED | 승인 | 근무신청 상태: 승인됨 |
| **WS03** | REJECTED | 반려 | 근무신청 상태: 반려됨 |
| **WS04** | CANCELLED | 취소 | 근무신청 상태: 취소됨 |
| **CR01** | EDIT | 수정 요청 | 요청 유형: 수정 |
| **CR02** | DELETE | 삭제 요청 | 요청 유형: 삭제 |
| **CS01** | PENDING | 대기 | 요청 상태: 대기중 |
| **CS02** | APPROVED | 승인 | 요청 상태: 승인됨 |
| **CS03** | REJECTED | 거절 | 요청 상태: 거부됨 |
| **CT01** | CHECK_IN | 출근 체크 | 출근 인증 타입: 출근 |
| **CT02** | CHECK_OUT | 퇴근 체크 | 출근 인증 타입: 퇴근 |
| **TT01** | REGULAR | 정기 업무 | 업무 유형: 정기 |
| **TT02** | IRREGULAR | 비정기 업무 | 업무 유형: 비정기 |
| **RL01** | STUDENT | 학생 | 역할: 학생 |
| **RL02** | ADMIN | 관리자 | 역할: 관리자 |

---

## 2. User & Organization

### 2.1 `organization`
조직 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| organization_id | INT | PK, AUTO_INCREMENT | 조직 ID |
| name | VARCHAR(100) | NOT NULL | 조직명 |
| description | TEXT | NULL | 조직 설명 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NOT NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

---

### 2.2 `user`
사용자 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | INT | PK, AUTO_INCREMENT | 사용자 ID |
| organization_id | INT | FK, NOT NULL | 소속 조직 ID |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (암호화) |
| name | VARCHAR(50) | NOT NULL | 사용자명 |
| role_code | CHAR(4) | FK, NOT NULL | 역할 코드 (RL01, RL02) |
| refresh_token | VARCHAR(512) | NULL | 리프레시 토큰 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

**Indexes**:
- `uq_user_email` (UNIQUE): `email`

**Relationships**:
- `organization_id` → `organization(organization_id)`
- `role_code` → `code(full_code)`
- One-to-Many with `work_schedule`
- One-to-Many with `work_attendance`
- One-to-Many with `work_change_request`
- One-to-Many with `manager_sub_category` (as manager)
- One-to-Many with `faq` (as writer/last_editor)

---

### 2.3 `manager_subcategory`
소분류 담당자 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | 매니저 소분류 ID |
| user_id | INT | FK, NOT NULL | 사용자 ID (매니저) |
| subcategory_id | BIGINT | FK, NOT NULL | 소분류 ID |
| assigned_at | TIMESTAMP | NOT NULL | 담당 지정일시 |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성화 여부 |

**Relationships**:
- `user_id` → `user(user_id)`
- `subcategory_id` → `sub_category(id)`

---

## 3. Work Schedule System

### 3.1 `work_schedule`
근무 일정 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| schedule_id | INT | PK, AUTO_INCREMENT | 일정 ID |
| user_id | INT | FK, NOT NULL | 사용자 ID |
| start_time | TIMESTAMP | NOT NULL | 시작 시간 |
| end_time | TIMESTAMP | NOT NULL | 종료 시간 |
| status_code | CHAR(4) | FK, NOT NULL | 상태 코드 (WS01~WS04) |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | 삭제 여부 (소프트 삭제) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NOT NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

**Relationships**:
- `user_id` → `user(user_id)`
- `status_code` → `code(full_code)`
- One-to-Many with `work_attendance`
- One-to-Many with `work_change_request`

---

### 3.2 `monthly_schedule_config`
월별 스케줄 설정 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| limit_id | INT | PK, AUTO_INCREMENT | 설정 ID |
| schedule_year | INT | NOT NULL | 대상 연도 |
| schedule_month | INT | NOT NULL | 대상 월 (1-12) |
| max_concurrent | INT | NOT NULL | 최대 동시 근무 인원수 |
| apply_start_time | TIMESTAMP | NOT NULL | 신청 시작 시간 |
| apply_end_time | TIMESTAMP | NOT NULL | 신청 종료 시간 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NOT NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

**Unique Constraint**:
- `UNIQUE(schedule_year, schedule_month)`

**기본 신청 기간 규칙**:
- 해당 월의 전달 23일 00:00 ~ 27일 00:00
- 예: 2025년 12월 설정 → 2025년 11월 23일 00:00 ~ 27일 00:00

---

### 3.3 `work_attendance`
출근 기록 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| attendance_id | INT | PK, AUTO_INCREMENT | 출근 기록 ID |
| user_id | INT | FK, NOT NULL | 사용자 ID |
| schedule_id | INT | FK, NOT NULL | 근무 일정 ID |
| check_time | TIMESTAMP | NOT NULL | 체크 시간 |
| check_type_code | CHAR(4) | FK, NOT NULL | 체크 타입 (CT01, CT02) |
| verified | BOOLEAN | DEFAULT FALSE | 검증 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

**Indexes**:
- `idx_wa_user_time`: `user_id`, `check_time`
- `idx_wa_schedule`: `schedule_id`

**Relationships**:
- `user_id` → `user(user_id)`
- `schedule_id` → `work_schedule(schedule_id)`
- `check_type_code` → `code(full_code)`

---

### 3.4 `work_change_request`
근무 변경 요청 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| request_id | INT | PK, AUTO_INCREMENT | 요청 ID |
| user_id | INT | FK, NOT NULL | 사용자 ID |
| schedule_id | INT | FK, NOT NULL | 근무 일정 ID |
| type_code | CHAR(4) | FK, NOT NULL | 요청 유형 (CR01, CR02) |
| reason | TEXT | NULL | 요청 사유 |
| status_code | CHAR(4) | FK, NOT NULL | 상태 코드 (CS01~CS03) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| created_by | INT | NOT NULL | 생성자 ID |
| updated_at | TIMESTAMP | NULL | 수정일시 |
| updated_by | INT | NULL | 수정자 ID |

**Indexes**:
- `idx_wcr_user`: `user_id`
- `idx_wcr_schedule`: `schedule_id`

**Relationships**:
- `user_id` → `user(user_id)`
- `schedule_id` → `work_schedule(schedule_id)`
- `type_code` → `code(full_code)`
- `status_code` → `code(full_code)`

---

## 4. Category & FAQ System

### 4.1 `category`
FAQ 대분류 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | 대분류 ID |
| name | VARCHAR(100) | NOT NULL | 대분류명 |

**Relationships**:
- One-to-Many with `sub_category`

---

### 4.2 `sub_category`
FAQ 소분류 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | 소분류 ID |
| category_id | BIGINT | FK, NOT NULL | 대분류 ID |
| name | VARCHAR(100) | NOT NULL | 소분류명 |
| favorite | BOOLEAN | NOT NULL, DEFAULT FALSE | 즐겨찾기 여부 |

**Relationships**:
- `category_id` → `category(id)`
- One-to-Many with `faq`
- One-to-Many with `manager_sub_category` (as subCategory)

---

### 4.3 `faq`
FAQ 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | FAQ ID |
| sub_category_id | BIGINT | FK, NOT NULL | 소분류 ID |
| title | VARCHAR(100) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| etc | TEXT | NULL | 기타 정보 |
| attachment_url | VARCHAR(150) | NULL | 첨부파일 URL |
| writer_name | VARCHAR(30) | NOT NULL | 작성자명 |
| last_edited_at | TIMESTAMP | NOT NULL | 마지막 수정일시 |
| last_editor_name | VARCHAR(30) | NOT NULL | 마지막 수정자명 |
| manager | VARCHAR(30) | NOT NULL | 담당자명 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| deleted_flag | BOOLEAN | NOT NULL | 삭제 여부 (소프트 삭제) |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |
| writer_id | INT | FK, NOT NULL | 작성자 ID |
| last_editor_id | INT | FK, NOT NULL | 마지막 수정자 ID |

**Relationships**:
- `sub_category_id` → `sub_category(id)`
- `writer_id` → `user(user_id)`
- `last_editor_id` → `user(user_id)`
- One-to-Many with `faq_history`

---

### 4.4 `faq_history`
FAQ 수정 이력 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| faq_id | BIGINT | FK, NOT NULL | FAQ ID |
| category | VARCHAR(100) | NOT NULL | 대분류명 |
| sub_category | VARCHAR(100) | NOT NULL | 소분류명 |
| title | VARCHAR(100) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| attachment_url | VARCHAR(150) | NULL | 첨부파일 URL |
| manager | VARCHAR(30) | NOT NULL | 담당자명 |
| writer_name | VARCHAR(30) | NOT NULL | 작성자명 |
| editor_name | VARCHAR(30) | NOT NULL | 수정자명 |
| edited_at | TIMESTAMP | NOT NULL | 수정일시 |

**Relationships**:
- `faq_id` → `faq(id)`

---

## 5. Email Verification

### 5.1 `email_verification_code`
이메일 인증 코드 테이블

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | 인증 코드 ID |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 |
| code | VARCHAR(6) | NOT NULL | 인증 코드 (6자리) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| expires_at | TIMESTAMP | NOT NULL | 만료일시 (기본 5분) |
| verified | BOOLEAN | NOT NULL, DEFAULT FALSE | 인증 여부 |
| attempt_count | INT | NOT NULL, DEFAULT 0 | 인증 실패 횟수 |

**Indexes**:
- `idx_code`: `code`
- `idx_email`: `email`

**Business Rules**:
- 이메일당 하나의 유효한 인증 코드만 존재 (UNIQUE 제약)
- 5분 후 코드 자동 만료
- 5회 이상 인증 실패 시 코드 무효화
- 코드는 6자리 랜덤 숫자 (000000 ~ 999999)

---

## Entity Relationships Summary

```
organization (1) ────── (*) user
                   │
                   └─ (*) manager_subcategory ── (*) sub_category
                          (1)                     │
                                                 └─ (*) category

user (1) ──────── (*) work_schedule ────── (*) work_attendance
│                    │
│                    └─ (*) work_change_request
│
└─ (*) faq (as writer/last_editor) ── (*) faq_history
│
└─ (*) manager_subcategory ── (*) sub_category ── (*) faq
                          (1)                     │
                                                 └─ (*) category

code (1) ────── (*) work_schedule (status_code)
(1) ────── (*) work_change_request (type_code, status_code)
(1) ────── (*) work_attendance (check_type_code)
(1) ────── (*) user (role_code)
```

---

## Common Fields

### Audit Fields (감사 필드)
대부분의 테이블에는 다음 감사 필드가 포함됩니다:
- `created_at`: 생성일시
- `created_by`: 생성자 ID
- `updated_at`: 수정일시
- `updated_by`: 수정자 ID

### Soft Delete (소프트 삭제)
다음 테이블은 소프트 삭제를 지원합니다:
- `work_schedule`: `is_deleted` 필드
- `faq`: `deleted_flag`, `deleted_at` 필드

---

## Database Indexes

| Table | Index Name | Columns | Type |
|-------|------------|---------|------|
| code | uq_code_major_sub | major_code, sub_code | UNIQUE |
| user | uq_user_email | email | UNIQUE |
| work_attendance | idx_wa_user_time | user_id, check_time | INDEX |
| work_attendance | idx_wa_schedule | schedule_id | INDEX |
| work_change_request | idx_wcr_user | user_id | INDEX |
| work_change_request | idx_wcr_schedule | schedule_id | INDEX |
| monthly_schedule_config | - | schedule_year, schedule_month | UNIQUE |

---

## Data Integrity Rules

### Business Rules
1. **월별 스케줄 설정**: `schedule_year`와 `schedule_month`의 조합은 유일해야 합니다.
2. **이메일 중복**: 사용자 이메일은 중복될 수 없습니다.
3. **코드 중복**: `major_code`와 `sub_code`의 조합은 유일해야 합니다.
4. **삭제 제약**:
   - `category` 삭제 시 하위 `sub_category`가 있으면 삭제 불가
   - `sub_category` 삭제 시 하위 `faq`가 있으면 삭제 불가
5. **신청 기간**: `apply_start_time`은 `apply_end_time`보다 이전이어야 합니다.
6. **변경 요청 처리**: `process-change-request` API 호출 시 `requestIds` 개수는 짝수여야 합니다 (쌍으로 처리).

### Cascade Rules
- `category` → `sub_category`: CASCADE ALL
- `sub_category` → `faq`: NOT CASCADING (삭제 제약 적용)
- `faq` → `faq_history`: NOT CASCADING (이력 유지)
- `user` → `manager_sub_category`: CASCADE ALL

---

## Performance Considerations

### Recommended Indexes
1. **조회 최적화**:
   - `work_schedule`: `user_id`, `start_time`, `status_code`에 복합 인덱스 고려
   - `faq`: `sub_category_id`, `deleted_flag`에 복합 인덱스 고려
   - `faq_history`: `faq_id`에 인덱스

2. **검색 최적화**:
   - `faq`: `title`, `content`에 FULLTEXT 인덱스 고려 (검색 기능 지원 시)

3. **날짜 범위 쿼리**:
   - `work_schedule`: `start_time`, `end_time`에 인덱스 고려
   - `work_attendance`: `check_time`에 인덱스
