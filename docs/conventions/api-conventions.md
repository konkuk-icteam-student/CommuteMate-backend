# API 설계 규약

## 📑 목차
- [개요](#-개요)
- [REST API 원칙](#-rest-api-원칙)
- [Request/Response 구조](#-requestresponse-구조)
- [네이밍 규칙](#-네이밍-규칙)
- [HTTP 메서드 사용 규칙](#-http-메서드-사용-규칙)
- [페이징 및 정렬](#-페이징-및-정렬)
- [날짜/시간 형식](#-날짜시간-형식)
- [JWT 인증](#-jwt-인증)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 프로젝트의 REST API 설계 표준을 정의합니다.
일관된 API 인터페이스를 제공하고 클라이언트 개발 효율성을 높이기 위한 규칙들입니다.

### 핵심 원칙
- **RESTful 설계**: 리소스 중심의 URL 구조
- **일관된 응답 형식**: `Response<T>` 래퍼 사용
- **타입 안전성**: CodeType Enum 활용
- **명확한 네이밍**: 직관적이고 예측 가능한 이름

---

## 🌐 REST API 원칙

### 1. 리소스 중심 URL

**원칙**: URL은 동사가 아닌 **명사(리소스)**로 구성

✅ **올바른 예시**:
```
GET    /api/v1/work-schedules         # 일정 목록 조회
POST   /api/v1/work-schedules         # 일정 생성
GET    /api/v1/work-schedules/{id}    # 특정 일정 조회
PUT    /api/v1/work-schedules/{id}    # 일정 수정
DELETE /api/v1/work-schedules/{id}    # 일정 삭제
```

❌ **잘못된 예시**:
```
GET    /api/v1/getSchedules      # 동사 사용 금지
POST   /api/v1/createSchedule    # 동사 사용 금지
POST   /api/v1/schedule/delete   # POST + delete 혼용 금지
```

### 2. 계층적 URL 구조

**원칙**: 리소스 간 관계를 URL로 표현

✅ **올바른 예시**:
```
GET /api/v1/users/{userId}/schedules        # 특정 사용자의 일정 목록
GET /api/v1/work-schedules/{scheduleId}/attendance  # 특정 일정의 출퇴근 기록
```

### 3. 버전 관리

**원칙**: URL에 API 버전 포함 (`/api/v1/`, `/api/v2/`)

```
/api/v1/work-schedules   # 버전 1
/api/v2/work-schedules   # 버전 2 (호환성 깨질 때)
```

### 4. 복수형 사용

**원칙**: 리소스 이름은 복수형 사용

✅ **올바른 예시**:
```
/api/v1/users
/api/v1/work-schedules
/api/v1/organizations
```

❌ **잘못된 예시**:
```
/api/v1/user
/api/v1/schedule
```

---

## 📋 Request/Response 구조

### Response 표준 형식

모든 API 응답은 `Response<T>` 래퍼를 사용합니다.

**성공 응답**:
```json
{
  "isSuccess": true,
  "message": "성공 메시지",
  "details": {
    // 실제 응답 데이터 (T)
  }
}
```

**실패 응답**:
```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "timestamp": "2025-01-22T14:30:00"
  }
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 | 비고 |
|------|------|------|------|
| `isSuccess` | Boolean | 성공 여부 | `true`: 성공, `false`: 실패 |
| `message` | String | 사용자에게 표시할 메시지 | 성공/실패 모두 포함 |
| `details` | Generic `<T>` | 응답 상세 데이터 | 성공 시: 실제 데이터, 실패 시: ErrorResponseDetail |

### Request DTO 규칙

**네이밍**: `{동작}{리소스}Request`

**예시**:
- `RegisterRequest` - 회원가입 요청
- `LoginRequest` - 로그인 요청
- `ApplyWorkSchedule` - 근무 일정 신청 (관사 생략 가능)
- `SetMonthlyLimitRequest` - 월별 제한 설정 요청

**필수 검증**:
- `@NotNull`, `@NotBlank`, `@NotEmpty` 활용
- `@Valid` 중첩 검증
- Custom Validator 필요 시 `@Constraint` 활용

**예시**:
```java
public class RegisterRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotNull(message = "역할 코드는 필수입니다.")
    private CodeType roleCode;  // CodeType Enum 사용
}
```

### Response DTO 규칙

**네이밍**: `{리소스}{설명}Response` 또는 `{리소스}DTO`

**예시**:
- `LoginResponse` - 로그인 응답 (토큰 포함)
- `MonthlyLimitResponse` - 월별 제한 응답
- `WorkScheduleDTO` - 근무 일정 데이터

**CodeType 사용**:
```java
public class WorkScheduleDTO {
    private Long scheduleId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private CodeType statusCode;  // ✅ CodeType Enum 사용

    // ❌ private String statusCode;  // 문자열 사용 금지
}
```

---

## 🏷️ 네이밍 규칙

### URL 네이밍

**원칙**: 소문자 + 하이픈 (kebab-case)

✅ **올바른 예시**:
```
/api/v1/work-schedules
/api/v1/monthly-schedule-limits
/api/v1/attendance
```

❌ **잘못된 예시**:
```
/api/v1/workSchedules       # camelCase 금지
/api/v1/work_schedules      # snake_case 금지
/api/v1/WorkSchedules       # PascalCase 금지
```

### 쿼리 파라미터 네이밍

**원칙**: camelCase 사용

✅ **올바른 예시**:
```
GET /api/v1/work-schedules?year=2025&month=11
GET /api/v1/work-schedules?userId=123&statusCode=WS02
GET /api/v1/users?page=1&size=10&sortBy=createdAt
```

### Request/Response 필드 네이밍

**원칙**: camelCase 사용

✅ **올바른 예시**:
```json
{
  "userId": 123,
  "roleCode": "RL01",
  "scheduleDate": "2025-11-15",
  "startTime": "09:00:00",
  "maxConcurrent": 10
}
```

❌ **잘못된 예시**:
```json
{
  "user_id": 123,           // snake_case 금지
  "RoleCode": "RL01",       // PascalCase 금지
  "schedule-date": "2025-11-15"  // kebab-case 금지
}
```

---

## 🔧 HTTP 메서드 사용 규칙

### 메서드별 용도

| 메서드 | 용도 | Idempotent | Safe | 예시 |
|--------|------|-----------|------|------|
| **GET** | 조회 | ✅ | ✅ | 일정 목록, 사용자 정보 |
| **POST** | 생성, 복잡한 조회 | ❌ | ❌ | 회원가입, 일정 신청 |
| **PUT** | 전체 수정 | ✅ | ❌ | 일정 전체 업데이트 |
| **PATCH** | 부분 수정 | ✅ | ❌ | 일정 시간만 수정 |
| **DELETE** | 삭제 | ✅ | ❌ | 일정 삭제 |

### 메서드 선택 가이드

**GET - 조회**:
```
GET /api/v1/work-schedules           # 목록 조회
GET /api/v1/work-schedules/{id}      # 상세 조회
GET /api/v1/work-schedules?year=2025&month=11  # 필터링 조회
```

**POST - 생성**:
```
POST /api/v1/auth/register      # 회원가입
POST /api/v1/work-schedules          # 일정 신청 (배치 가능)
POST /api/v1/attendance/check-in  # 출근 체크
```

**PUT - 전체 수정**:
```
PUT /api/v1/work-schedules/{id}      # 일정 전체 업데이트
PUT /api/v1/users/{id}          # 사용자 정보 전체 수정
```

**PATCH - 부분 수정**:
```
PATCH /api/v1/work-schedules/{id}    # 일정 시간만 수정
PATCH /api/v1/tasks/{id}/complete  # 업무 완료 상태만 변경
```

**DELETE - 삭제**:
```
DELETE /api/v1/work-schedules/{id}   # 일정 삭제
DELETE /api/v1/tasks/{id}       # 업무 삭제
```

### 특수 케이스

**배치 작업**: POST 사용
```
POST /api/v1/work-schedules          # 여러 일정 일괄 신청
POST /api/v1/tasks/batch        # 업무 일괄 생성
```

**복잡한 조회**: POST 사용 (Body로 복잡한 필터 전달)
```
POST /api/v1/work-schedules/search   # 복잡한 검색 조건
POST /api/v1/users/query        # 복잡한 사용자 쿼리
```

---

## 📄 페이징 및 정렬

### 페이징 파라미터

**표준 파라미터**:
- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기 (기본값: 10, 최대: 100)

**예시**:
```
GET /api/v1/work-schedules?page=0&size=20
GET /api/v1/users?page=2&size=50
```

### 정렬 파라미터

**표준 파라미터**:
- `sortBy`: 정렬 기준 필드
- `direction`: 정렬 방향 (`asc`, `desc`)

**예시**:
```
GET /api/v1/work-schedules?sortBy=scheduleDate&direction=desc
GET /api/v1/users?sortBy=createdAt&direction=asc
```

### 페이징 응답 형식

**Page 정보 포함**:
```json
{
  "isSuccess": true,
  "message": "일정 목록 조회 성공",
  "details": {
    "content": [
      { "scheduleId": 1, ... },
      { "scheduleId": 2, ... }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

## 📅 날짜/시간 형식

### ISO-8601 표준 사용

| 타입 | 형식 | 예시 | Java 타입 |
|------|------|------|-----------|
| **날짜** | `yyyy-MM-dd` | `2025-11-15` | `LocalDate` |
| **시간** | `HH:mm:ss` | `14:30:00` | `LocalTime` |
| **날짜+시간** | `yyyy-MM-dd'T'HH:mm:ss` | `2025-11-15T14:30:00` | `LocalDateTime` |
| **타임스탬프** | Unix timestamp (초) | `1736950200` | `long` (변환) |

### Request 예시

```json
{
  "scheduleDate": "2025-11-15",
  "startTime": "09:00:00",
  "endTime": "18:00:00"
}
```

### Response 예시

```json
{
  "scheduleId": 1,
  "scheduleDate": "2025-11-15",
  "startTime": "09:00:00",
  "endTime": "18:00:00",
  "createdAt": "2025-11-01T10:30:00"
}
```

---

## 🔐 JWT 인증

### 토큰 타입

| 토큰 | 용도 | 전달 방식 | 유효 시간 |
|------|------|----------|----------|
| **AccessToken** | API 요청 인증 | `Authorization` 헤더 | 1시간 |
| **RefreshToken** | AccessToken 갱신 | `Authorization` 헤더 | 7일 |

### 헤더 형식

**AccessToken / RefreshToken**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 인증 흐름

```
1. POST /api/v1/auth/login
   → AccessToken + RefreshToken 발급
   → AccessToken은 HttpOnly Cookie로도 설정되지만, 인증 필터는 Authorization 헤더만 사용

2. GET /api/v1/tasks (인증 필요)
   → Authorization 헤더로 AccessToken 전달
   → 서버에서 검증 후 응답

3. AccessToken 만료 시
   → POST /api/v1/auth/refresh (RefreshToken 전송)
   → 새로운 AccessToken + RefreshToken 발급

4. POST /api/v1/auth/logout
   → AccessToken 블랙리스트 등록
   → 토큰 무효화
```

**자세한 내용**: [인증 API 문서](../api/auth.md)

---

## 💻 사용 예시

### 예시 1: 근무 일정 신청 (배치)

**Request**:
```http
POST /api/v1/work-schedules
Content-Type: application/json

[
  {
    "scheduleDate": "2025-11-15",
    "startTime": "09:00:00",
    "endTime": "18:00:00"
  },
  {
    "scheduleDate": "2025-11-16",
    "startTime": "09:00:00",
    "endTime": "18:00:00"
  }
]
```

**Response (200 OK - 모두 성공)**:
```json
{
  "isSuccess": true,
  "message": "모든 일정 신청이 완료되었습니다.",
  "details": {
    "success": [
      { "scheduleId": 1, "scheduleDate": "2025-11-15", ... },
      { "scheduleId": 2, "scheduleDate": "2025-11-16", ... }
    ],
    "failure": []
  }
}
```

**Response (207 Multi-Status - 부분 성공)**:
```json
{
  "isSuccess": false,
  "message": "일부 일정 신청이 실패했습니다.",
  "details": {
    "success": [
      { "scheduleId": 1, "scheduleDate": "2025-11-15", ... }
    ],
    "failure": [
      { "scheduleDate": "2025-11-16", "reason": "월별 제한 초과" }
    ]
  }
}
```

### 예시 2: 월별 일정 조회 (페이징)

**Request**:
```http
GET /api/v1/work-schedules?year=2025&month=11&page=0&size=10&sortBy=scheduleDate&direction=asc
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "일정 목록 조회 성공",
  "details": {
    "content": [
      {
        "scheduleId": 1,
        "scheduleDate": "2025-11-15",
        "startTime": "09:00:00",
        "endTime": "18:00:00",
        "statusCode": "WS02"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 30,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

### 예시 3: 회원가입 (CodeType 사용)

**Request**:
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "roleCode": "RL01",
  "organizationId": 1
}
```

**Response (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "회원가입이 완료되었습니다.",
  "details": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "roleCode": "RL01"
  }
}
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [에러 처리 규약](./error-handling.md) - 에러 응답 형식
- **필수**: [API 문서](../api/README.md) - 실제 API 구현 예시
- **참고**: [코드 시스템](../database/schema/code-system.md) - CodeType Enum 상세

### 상위/하위 문서
- ⬆️ **상위**: [개발 규약 홈](./README.md)
- ➡️ **관련**:
  - [에러 처리 규약](./error-handling.md)
  - [코딩 스타일](./code-style.md)

### 도메인별 API 문서
- [인증 API](../api/auth.md) - JWT 인증 흐름
- [근무 일정 API](../api/schedule.md) - 일정 신청 및 조회
- [출퇴근 API](../api/attendance.md) - QR 체크인/체크아웃
- [관리자 API](../api/admin.md) - 월별 제한 관리

### 관련 파일
- **Response 래퍼**: `global/controller/dtos/Response.java`
- **Request 예시**: `auth/controller/dto/RegisterRequest.java`
- **Response 예시**: `auth/controller/dto/LoginResponse.java`
- **CodeType**: `global/code/CodeType.java`
