# CommuteMate API Documentation

## Overview
CommuteMate 백엔드 시스템의 REST API 엔드포인트 문서입니다.
모든 날짜 및 시간 형식은 ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`)을 따릅니다.

## Base URL
```
http://localhost:8080
```

## Authentication
대부분의 API는 JWT 인증이 필요합니다.
- **AccessToken**: `HttpOnly Cookie`로 전달됩니다. (브라우저 자동 처리)
- **RefreshToken**: `Authorization` 헤더로 전달해야 합니다.
  - Format: `Bearer <token>`

---

## 1. Authentication API (`/api/v1/auth`)

### 1.1 Send Verification Code
이메일 인증 번호를 발송합니다.
- **POST** `/api/v1/auth/send-verification-code`

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "인증번호가 발송되었습니다.",
  "details": null
}
```

### 1.2 Verify Code
이메일 인증 번호를 검증합니다.
- **POST** `/api/v1/auth/verify-code`

**Request Body**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

### 1.3 Register
회원가입을 진행합니다.
- **POST** `/api/v1/auth/register`

**Request Body**
```json
{
  "email": "newuser@example.com",
  "password": "securePass123",
  "name": "홍길동",
  "roleCode": "RL01", 
  "organizationId": 1
}
```
> `roleCode`: `RL01`(학생/사원), `RL02`(관리자)

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "회원가입이 완료되었습니다.",
  "details": {
    "userId": 1,
    "email": "newuser@example.com"
  }
}
```

### 1.4 Login
로그인하여 액세스 토큰(쿠키)과 리프레시 토큰을 발급받습니다.
- **POST** `/api/v1/auth/login`

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "로그인 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresAt": 1736560000
  }
}
```

### 1.5 Logout
로그아웃합니다 (토큰 블랙리스트 등록).
- **POST** `/api/v1/auth/logout`

### 1.6 Refresh Token
리프레시 토큰으로 액세스 토큰을 갱신합니다.
- **POST** `/api/v1/auth/refresh`
- **Header**: `Authorization: Bearer <refreshToken>`

---

## 2. Work Schedule API (`/api/v1/work-schedules`)

### 2.1 Apply Work Schedule
근무 일정을 일괄 신청합니다.
- **POST** `/api/v1/work-schedules/apply`

**Request Body**
```json
{
  "slots": [
    {
      "start": "2026-01-11T09:00:00",
      "end": "2026-01-11T12:00:00"
    },
    {
      "start": "2026-01-11T13:00:00",
      "end": "2026-01-11T18:00:00"
    }
  ]
}
```

**Response (201 Created)** - 전체 성공
```json
{
  "isSuccess": true,
  "message": "일정 신청 성공",
  "details": {
    "success": [
      { "start": "2026-01-11T09:00:00", "end": "2026-01-11T12:00:00" },
      { "start": "2026-01-11T13:00:00", "end": "2026-01-11T18:00:00" }
    ],
    "fail": []
  }
}
```

**Response (207 Multi-Status)** - 부분 성공
```json
{
  "isSuccess": true,
  "message": "일부 일정 신청 실패",
  "details": {
    "success": [ ... ],
    "fail": [
      { "start": "2026-01-11T13:00:00", "end": "2026-01-11T18:00:00" }
    ]
  }
}
```

### 2.2 Modify Work Schedule
기존 일정을 취소하고 새로운 일정을 추가합니다.
- **PATCH** `/api/v1/work-schedules/modify`

**Request Body**
```json
{
  "cancelScheduleIds": [101, 102],
  "newSlots": [
    {
      "start": "2026-01-12T09:00:00",
      "end": "2026-01-12T18:00:00"
    }
  ]
}
```

### 2.3 Get My Schedules
나의 근무 일정을 월별로 조회합니다.
- **GET** `/api/v1/work-schedules?year=2026&month=1`

### 2.4 Get My History
나의 지난 근무 이력을 조회합니다.
- **GET** `/api/v1/work-schedules/history?year=2026&month=1`

---

## 3. Admin Schedule API (`/api/v1/admin/schedule`)

### 3.1 Set Monthly Limit
월별 최대 동시 근무 인원을 설정합니다.
- **POST** `/api/v1/admin/schedule/monthly-limit`

**Request Body**
```json
{
  "scheduleYear": 2026,
  "scheduleMonth": 1,
  "maxConcurrent": 15
}
```

### 3.2 Get Monthly Limit
- **GET** `/api/v1/admin/schedule/monthly-limit/{year}/{month}`

### 3.3 Set Apply Term
근무 신청 기간을 설정합니다.
- **POST** `/api/v1/admin/schedule/set-apply-term`

**Request Body**
```json
{
  "scheduleYear": 2026,
  "scheduleMonth": 1,
  "applyStartTime": "2025-12-23T00:00:00",
  "applyEndTime": "2025-12-27T00:00:00"
}
```

### 3.4 Process Change Request
근무 일정 변경 요청을 승인/거부합니다.
- **POST** `/api/v1/admin/schedule/process-change-request`

**Request Body**
```json
{
  "requestIds": [101, 102],
  "statusCode": "CS02"
}
```
> `statusCode`: `CS02`(승인), `CS03`(거절)

---

## 4. Attendance API (`/api/v1/attendance`)

### 4.1 Get QR Token
관리자 태블릿용 QR 생성 토큰을 발급합니다.
- **GET** `/api/v1/attendance/qr-token`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "QR 토큰 발급 성공",
  "details": {
    "token": "random_secure_token_string",
    "expiresAt": "2026-01-11T12:01:00",
    "validSeconds": 60
  }
}
```

### 4.2 Check In / Out
QR 코드를 통해 출근/퇴근 체크를 합니다.
- **POST** `/api/v1/attendance/check-in`
- **POST** `/api/v1/attendance/check-out`

**Request Body**
```json
{
  "qrToken": "random_secure_token_string"
}
```

### 4.3 Get History
특정 날짜의 출석 기록을 조회합니다.
- **GET** `/api/v1/attendance/history?date=2026-01-11`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "조회 성공",
  "details": {
    "histories": [
      {
        "attendanceId": 501,
        "checkTime": "2026-01-11T08:55:00",
        "checkType": "CT01",
        "scheduleId": 101,
        "scheduleStartTime": "2026-01-11T09:00:00",
        "scheduleEndTime": "2026-01-11T18:00:00"
      }
    ]
  }
}
```

---

## 5. User API (`/api/v1/users`)

### 5.1 Get My Info
- **GET** `/api/v1/users/me`

### 5.2 Get Work Time Stats
- **GET** `/api/v1/users/me/work-time/weekly`
- **GET** `/api/v1/users/me/work-time/monthly`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "조회 성공",
  "details": {
    "totalMinutes": 2400,
    "periodType": "MONTHLY"
  }
}
```

---

## 6. Home API (`/api/v1/home`)

### 6.1 Get Home Work Time
오늘의 근무 시간 요약.
- **GET** `/api/v1/home/work-time`

**Response**
```json
{
  "details": {
    "totalMinutes": 480,
    "scheduleCount": 1
  }
}
```

---

## 7. FAQ & Category API

### 7.1 Register Category
대분류 등록.
- **POST** `/api/v1/categories`

**Request Body**
```json
{
  "categoryName": "인사관리"
}
```

### 7.2 Register SubCategory
소분류 등록.
- **POST** `/api/v1/subcategories`

**Request Body**
```json
{
  "subCategoryName": "휴가",
  "categoryId": 1
}
```

### 7.3 Create FAQ
FAQ 게시글 생성.
- **POST** `/v1/faq`

**Request Body**
```json
{
  "userId": 1,
  "category": "인사관리",
  "subCategory": "휴가",
  "title": "연차 신청 방법",
  "content": "시스템에서 신청하시면 됩니다.",
  "attachmentUrl": "http://...",
  "etc": "참고 사항"
}
```

---

## 8. Manager API (`/api/v1/manager`)

### 8.1 Register Manager Mappings
매니저와 소분류 매핑.
- **POST** `/api/v1/manager`

**Request Body**
```json
{
  "managerId": 3,
  "subCategoryIds": [1, 2, 5]
}
```

---

## Error Response Format
모든 에러 응답은 아래 형식을 따릅니다.

```json
{
  "isSuccess": false,
  "message": "인증번호를 찾을 수 없습니다.",
  "details": {
    "errorCode": "VERIFICATION_CODE_NOT_FOUND",
    "timestamp": "2026-01-11T12:00:00"
  }
}
```
