# CommuteMate API Documentation

## Overview
CommuteMate 백엔드 시스템의 REST API 엔드포인트 문서입니다.
모든 날짜 및 시간 형식은 ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss` 또는 `yyyy-MM-dd`)을 따릅니다.

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

## 1. Auth API (`auth/`)
Base Path: `/api/v1/auth`

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
  "message": "인증번호가 이메일로 발송되었습니다. (유효시간: 5분)",
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

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.",
  "details": null
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
    "email": "newuser@example.com",
    "name": "홍길동",
    "roleCode": "RL01"
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
- **Header**: `Authorization: Bearer <accessToken>`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "로그아웃되었습니다.",
  "details": null
}
```

### 1.6 Refresh Token
리프레시 토큰으로 액세스 토큰을 갱신합니다.
- **POST** `/api/v1/auth/refresh`
- **Header**: `Authorization: Bearer <refreshToken>`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "토큰 갱신 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresAt": 1736560000
  }
}
```

**Error Response (Example)**
```json
{
  "isSuccess": false,
  "message": "유효하지 않은 토큰입니다.",
  "details": {
    "errorCode": "INVALID_TOKEN",
    "timestamp": "2026-01-11T12:00:00"
  }
}
```

---

## 2. Schedule API (`schedule/`)

### 2.1 Work Schedule API
Base Path: `/api/v1/work-schedules`

#### 2.1.1 Apply Work Schedule
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
  "message": "신청하신 일정이 모두 등록되었습니다.",
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

#### 2.1.2 Modify Work Schedule
기존 일정을 취소하고 새로운 일정을 추가합니다.
- **PATCH** `/api/v1/work-schedules/modify`

**Request Body**
```json
{
  "cancelScheduleIds": [101, 102],
  "applySlots": [
    {
      "start": "2026-01-12T09:00:00",
      "end": "2026-01-12T18:00:00"
    }
  ],
  "reason": "개인 사유"
}
```

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 수정(요청)되었습니다.",
  "details": null
}
```

#### 2.1.3 Get My Schedules
나의 근무 일정을 월별로 조회합니다.
- **GET** `/api/v1/work-schedules?year=2026&month=1`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "근무 일정 조회 성공",
  "details": {
    "workSchedules": [
      {
        "scheduleId": 101,
        "scheduleDate": "2026-01-11",
        "startTime": "09:00:00",
        "endTime": "18:00:00",
        "statusCode": "WS02"
      }
    ]
  }
}
```

#### 2.1.4 Get My History
나의 지난 근무 이력을 조회합니다.
- **GET** `/api/v1/work-schedules/history?year=2026&month=1`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "근무 이력 조회 성공",
  "details": {
    "histories": [
      {
        "scheduleId": 101,
        "date": "2026-01-11",
        "scheduledTime": "09:00~18:00",
        "actualTime": "08:55~18:05",
        "status": "APPROVED"
      }
    ]
  }
}
```

#### 2.1.5 Get Work Schedule Detail
특정 근무 일정 상세 조회.
- **GET** `/api/v1/work-schedules/{scheduleId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "근무 일정 상세 조회 성공",
  "details": {
    "scheduleId": 101,
    "scheduleDate": "2026-01-11",
    "startTime": "09:00:00",
    "endTime": "18:00:00",
    "statusCode": "WS02"
  }
}
```

#### 2.1.6 Delete Work Schedule
근무 일정 취소/삭제.
- **DELETE** `/api/v1/work-schedules/{scheduleId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "근무 일정 취소(요청) 성공",
  "details": null
}
```

### 2.2 Admin Schedule API
Base Path: `/api/v1/admin/schedule`

#### 2.2.1 Set Monthly Limit
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

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한이 설정되었습니다.",
  "details": {
    "scheduleYear": 2026,
    "scheduleMonth": 1,
    "maxConcurrent": 15
  }
}
```

#### 2.2.2 Get Monthly Limit
- **GET** `/api/v1/admin/schedule/monthly-limit/{year}/{month}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한을 조회했습니다.",
  "details": {
    "scheduleYear": 2026,
    "scheduleMonth": 1,
    "maxConcurrent": 15
  }
}
```

#### 2.2.3 Set Apply Term
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

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "신청 기간이 설정되었습니다.",
  "details": {
    "scheduleYear": 2026,
    "scheduleMonth": 1,
    "applyStartTime": "2025-12-23T00:00:00",
    "applyEndTime": "2025-12-27T00:00:00"
  }
}
```

#### 2.2.4 Process Change Request
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

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "변경 요청이 승인되었습니다.",
  "details": null
}
```

---

## 3. Attendance API (`attendance/`)
Base Path: `/api/v1/attendance`

### 3.1 Get QR Token
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

### 3.2 Check In
QR 코드를 통해 출근 체크를 합니다.
- **POST** `/api/v1/attendance/check-in`

**Request Body**
```json
{
  "qrToken": "random_secure_token_string"
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "출근 처리가 완료되었습니다.",
  "details": null
}
```

### 3.3 Check Out
QR 코드를 통해 퇴근 체크를 합니다.
- **POST** `/api/v1/attendance/check-out`

**Request Body**
```json
{
  "qrToken": "random_secure_token_string"
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "퇴근 처리가 완료되었습니다.",
  "details": null
}
```

### 3.4 Get Today History
오늘의 출퇴근 기록을 조회합니다.
- **GET** `/api/v1/attendance/today`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "오늘의 출퇴근 기록 조회 성공",
  "details": {
    "histories": [
      {
        "attendanceId": 501,
        "checkTime": "2026-01-11T08:55:00",
        "checkType": "CT01"
      }
    ]
  }
}
```

### 3.5 Get History (By Date)
특정 날짜의 출석 기록을 조회합니다.
- **GET** `/api/v1/attendance/history?date=2026-01-11`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "출퇴근 이력 조회 성공",
  "details": {
    "histories": [ ... ]
  }
}
```

---

## 4. User API (`user/`)
Base Path: `/api/v1/users`

### 4.1 Get My Info
- **GET** `/api/v1/users/me`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "내 정보 조회 성공",
  "details": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "roleCode": "RL01",
    "organizationName": "IT개발팀"
  }
}
```

### 4.2 Get Weekly Work Time
- **GET** `/api/v1/users/me/work-time/weekly`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "주간 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 2400,
    "periodType": "WEEKLY"
  }
}
```

### 4.3 Get Monthly Work Time
- **GET** `/api/v1/users/me/work-time/monthly`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "월간 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 9600,
    "periodType": "MONTHLY"
  }
}
```

---

## 5. Home API (`home/`)
Base Path: `/api/v1/home`

### 5.1 Get Today Work Time
오늘의 근무 시간 및 예정 스케줄 수 조회.
- **GET** `/api/v1/home/work-time`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "오늘의 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 480,
    "scheduleCount": 1
  }
}
```

### 5.2 Get Attendance Status
현재 출퇴근 버튼 상태 조회.
- **GET** `/api/v1/home/attendance-status`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "CHECK_IN_AVAILABLE",
    "lastCheckTime": null
  }
}
```

---

## 6. Task API (`task/`)
Base Path: `/api/v1/tasks`

### 6.1 Get Tasks By Date
특정 날짜의 업무 목록을 조회합니다.
- **GET** `/api/v1/tasks?date=2025-10-24`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무 목록을 조회했습니다.",
  "details": {
    "date": "2025-10-24",
    "regularTasks": [],
    "irregularTasks": []
  }
}
```

### 6.2 Get Task Detail
특정 업무의 상세 정보를 조회합니다.
- **GET** `/api/v1/tasks/{taskId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무를 조회했습니다.",
  "details": {
    "taskId": 50,
    "title": "서버 점검",
    "assigneeId": 101,
    "taskDate": "2025-10-24",
    "taskTime": "14:00:00",
    "taskType": "TT01",
    "isCompleted": false
  }
}
```

### 6.3 Create Task (Admin)
업무 생성 (관리자 전용).
- **POST** `/api/v1/tasks`

**Request Body**
```json
{
  "title": "서버 점검",
  "assigneeId": 101,
  "taskDate": "2025-10-24",
  "taskTime": "14:00:00",
  "taskType": "TT01"
}
```

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "업무가 생성되었습니다.",
  "details": {
    "taskId": 51,
    "title": "서버 점검",
    "assigneeId": 101,
    "taskDate": "2025-10-24",
    "taskTime": "14:00:00",
    "taskType": "TT01",
    "isCompleted": false
  }
}
```

### 6.4 Update Task (Admin)
업무 수정 (관리자 전용).
- **PATCH** `/api/v1/tasks/{taskId}`

**Request Body**
```json
{
  "title": "서버 점검 수정",
  "assigneeId": 102,
  "taskTime": "15:00:00"
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무가 수정되었습니다.",
  "details": { ... }
}
```

### 6.5 Toggle Complete (Admin)
업무 완료 상태 토글 (관리자 전용).
- **PATCH** `/api/v1/tasks/{taskId}/toggle-complete`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무를 완료 처리했습니다.",
  "details": {
    "taskId": 50,
    "isCompleted": true
  }
}
```

### 6.6 Set Complete (Admin)
업무 완료 상태 설정 (관리자 전용).
- **PATCH** `/api/v1/tasks/{taskId}/complete`

**Request Body**
```json
{
  "isCompleted": true
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무를 완료 처리했습니다.",
  "details": {
    "taskId": 50,
    "isCompleted": true
  }
}
```

### 6.7 Delete Task (Admin)
업무 삭제 (관리자 전용).
- **DELETE** `/api/v1/tasks/{taskId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무가 삭제되었습니다.",
  "details": null
}
```

### 6.8 Batch Update Tasks (Admin)
업무 일괄 생성/수정 (관리자 전용).
- **PUT** `/api/v1/tasks/batch`

**Request Body**
```json
{
  "date": "2025-10-24",
  "tasks": [
    {
      "taskId": 10,
      "title": "기존 업무 수정",
      "assigneeId": 101,
      "taskTime": "10:00:00",
      "taskType": "TT01",
      "isCompleted": false
    },
    {
      "taskId": null,
      "title": "새 업무 생성",
      "assigneeId": 102,
      "taskTime": "14:00:00",
      "taskType": "TT02",
      "isCompleted": false
    }
  ]
}
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "업무가 저장되었습니다. (생성: 1개, 수정: 1개)",
  "details": {
    "totalCreated": 1,
    "totalUpdated": 1,
    "totalErrors": 0,
    "errors": []
  }
}
```

### 6.9 Task Template API (`task/task-templates`)
Base Path: `/api/v1/task-templates`

#### 6.9.1 Get Templates
템플릿 목록 조회.
- **GET** `/api/v1/task-templates?activeOnly=false`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "템플릿 목록을 조회했습니다.",
  "details": {
    "templates": [
      {
        "templateId": 1,
        "templateName": "주간 회의 준비",
        "description": "...",
        "isActive": true
      }
    ]
  }
}
```

#### 6.9.2 Get Template Detail
템플릿 상세 조회.
- **GET** `/api/v1/task-templates/{templateId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "템플릿을 조회했습니다.",
  "details": {
    "templateId": 1,
    "templateName": "주간 회의 준비",
    "items": [
      {
        "title": "회의실 예약",
        "taskTime": "09:00:00"
      }
    ]
  }
}
```

#### 6.9.3 Create Template (Admin)
템플릿 생성 (관리자 전용).
- **POST** `/api/v1/task-templates`

**Request Body**
```json
{
  "templateName": "주간 회의 준비",
  "description": "매주 월요일 회의 준비 템플릿",
  "items": [
    {
      "title": "회의실 예약",
      "defaultAssigneeId": null,
      "taskTime": "09:00:00",
      "taskType": "TT01",
      "displayOrder": 1
    }
  ]
}
```

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "템플릿이 생성되었습니다.",
  "details": { ... }
}
```

#### 6.9.4 Update Template (Admin)
템플릿 수정 (관리자 전용).
- **PUT** `/api/v1/task-templates/{templateId}`

**Request Body**
```json
{
  "templateName": "주간 회의 준비 (수정)",
  "description": "설명 수정",
  "items": [ ... ] 
}
```

#### 6.9.5 Delete Template (Admin)
템플릿 삭제 (관리자 전용).
- **DELETE** `/api/v1/task-templates/{templateId}`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "템플릿이 삭제되었습니다.",
  "details": null
}
```

#### 6.9.6 Set Template Active (Admin)
템플릿 활성화/비활성화 (관리자 전용).
- **PATCH** `/api/v1/task-templates/{templateId}/active?isActive=true`

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "템플릿이 활성화되었습니다.",
  "details": { ... }
}
```

#### 6.9.7 Apply Template (Admin)
템플릿 적용하여 업무 생성 (관리자 전용).
- **POST** `/api/v1/task-templates/{templateId}/apply`

**Request Body**
```json
{
  "targetDate": "2025-10-25",
  "assigneeOverrides": [
    {
      "itemId": 5,
      "assigneeId": 105
    }
  ]
}
```

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "템플릿이 적용되어 3개의 업무가 생성되었습니다.",
  "details": {
    "createdCount": 3,
    "targetDate": "2025-10-25"
  }
}
```

---

## 7. Global API (`global/`)

### 7.1 WebSocket API
실시간 상태 변경 알림을 위한 웹소켓 API입니다.

- **Connection Endpoint**: `/ws` (SockJS, STOMP)
- **Topic**: `/topic/schedule-updates`

**Message Example**
```json
{
  "type": "SCHEDULE_UPDATED",
  "targetDate": "2026-01-11",
  "message": "근무 신청이 승인되었습니다."
}
```

---

## Error Response Format
모든 에러 응답은 아래 형식을 따릅니다.

```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "errorCode": "ERROR_CODE_ENUM",
    "timestamp": "2026-01-11T12:00:00"
  }
}
```