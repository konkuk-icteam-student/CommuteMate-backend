# 관리자 근무 일정 API (Admin Schedule)

## 📑 목차
- [개요](#-개요)
- [인증 및 권한](#-인증-및-권한)
- [주요 엔드포인트](#-주요-엔드포인트)
- [상세 엔드포인트](#-상세-엔드포인트)
  - [월별 제한 설정](#1️⃣-월별-제한-설정)
  - [신청 기간 설정](#2️⃣-신청-기간-설정)
  - [특정 월 제한 조회](#3️⃣-특정-월-제한-조회)
  - [모든 월별 제한 조회](#4️⃣-모든-월별-제한-조회)
  - [사용자 근무 시간 조회](#5️⃣-사용자-근무-시간-조회)
  - [전체 근무 시간 통계](#6️⃣-전체-근무-시간-통계)
  - [사용자 근무 이력 조회](#7️⃣-사용자-근무-이력-조회)
  - [전체 근무 이력 조회](#8️⃣-전체-근무-이력-조회)
  - [변경 요청 처리](#9️⃣-변경-요청-처리)
  - [승인 대기 신청 목록](#🔟-승인-대기-신청-목록)
- [관련 문서](#-관련-문서)

---

## 📖 개요

관리자용 근무 일정 설정 및 변경 요청 처리 API입니다.

**Base Path**: `/api/v1/admin/schedule`

---

## 🔐 인증 및 권한

- 현재 `SecurityConfig` 기준으로 인증이 강제되지 않지만,
  대부분 엔드포인트가 `@AuthenticationPrincipal`을 사용하므로 **Authorization 헤더가 필요**합니다.
- 관리자 권한 문자열: `ROLE_RL02` (`hasRole('RL02')`)

---

## 📊 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/monthly-limit` | 월별 최대 동시 근무 인원 설정 |
| POST | `/set-apply-term` | 근무 신청 가능 기간 설정 |
| GET | `/monthly-limit/{year}/{month}` | 특정 월 제한 조회 |
| GET | `/monthly-limits` | 모든 월별 제한 조회 |
| GET | `/work-time` | 특정 사용자 근무 시간 조회 |
| GET | `/work-time/summary` | 전체 근무 시간 통계 |
| GET | `/history` | 특정 사용자 근무 이력 조회 |
| GET | `/history/all` | 전체 근무 이력 조회 |
| POST | `/process-change-request` | 변경 요청 승인/거부 |
| GET | `/apply-requests` | 승인 대기 신청 목록 |

---

## 🔧 상세 엔드포인트

### 1️⃣ 월별 제한 설정

**Endpoint**: `POST /api/v1/admin/schedule/monthly-limit`

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "scheduleYear": 2025,
  "scheduleMonth": 12,
  "maxConcurrent": 15
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한이 설정되었습니다.",
  "details": {
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "maxConcurrent": 15
  }
}
```

---

### 2️⃣ 신청 기간 설정

**Endpoint**: `POST /api/v1/admin/schedule/set-apply-term`

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
※ 현재 구현은 사용자 ID를 인증 정보가 아닌 **고정값(1)**으로 사용합니다.

**Body**:
```json
{
  "scheduleYear": 2025,
  "scheduleMonth": 2,
  "applyStartTime": "2025-01-20T09:00:00",
  "applyEndTime": "2025-02-05T18:00:00"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "신청 기간이 설정되었습니다.",
  "details": {
    "scheduleYear": 2025,
    "scheduleMonth": 2,
    "applyStartTime": "2025-01-20T09:00:00",
    "applyEndTime": "2025-02-05T18:00:00"
  }
}
```

---

### 3️⃣ 특정 월 제한 조회

**Endpoint**: `GET /api/v1/admin/schedule/monthly-limit/{year}/{month}`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한을 조회했습니다.",
  "details": {
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "maxConcurrent": 15
  }
}
```

**Response (404 Not Found)**:
```json
{
  "isSuccess": false,
  "message": "해당 월의 스케줄 제한 설정을 찾을 수 없습니다.",
  "details": null
}
```

---

### 4️⃣ 모든 월별 제한 조회

**Endpoint**: `GET /api/v1/admin/schedule/monthly-limits`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "모든 월별 스케줄 제한을 조회했습니다.",
  "details": {
    "limits": [
      {
        "year": 2025,
        "month": 12,
        "maxConcurrent": 15
      }
    ]
  }
}
```

---

### 5️⃣ 사용자 근무 시간 조회

**Endpoint**: `GET /api/v1/admin/schedule/work-time?userId=1&year=2025&month=12`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "사용자 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 540,
    "periodType": "MONTHLY"
  }
}
```

---

### 6️⃣ 전체 근무 시간 통계

**Endpoint**: `GET /api/v1/admin/schedule/work-time/summary?year=2025&month=12`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "전체 근무 시간 통계 조회 성공",
  "details": {
    "summary": [
      {
        "userInfo": {
          "userId": 1,
          "email": "user@example.com",
          "name": "홍길동",
          "role": "RL01",
          "organizationId": 1
        },
        "totalMinutes": 540
      }
    ]
  }
}
```

---

### 7️⃣ 사용자 근무 이력 조회

**Endpoint**: `GET /api/v1/admin/schedule/history?userId=1&year=2025&month=12`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "사용자 근무 이력 조회 성공",
  "details": {
    "histories": [
      {
        "id": 123,
        "start": "2025-12-01T09:00:00",
        "end": "2025-12-01T12:00:00",
        "status": "WS02",
        "actualStart": "2025-12-01T09:05:00",
        "actualEnd": "2025-12-01T12:00:00",
        "workDurationMinutes": 175
      }
    ]
  }
}
```

---

### 8️⃣ 전체 근무 이력 조회

**Endpoint**: `GET /api/v1/admin/schedule/history/all?year=2025&month=12`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "전체 근무 이력 조회 성공",
  "details": {
    "histories": [
      {
        "id": 123,
        "userName": "홍길동",
        "start": "2025-12-01T09:00:00",
        "end": "2025-12-01T12:00:00",
        "status": "WS02",
        "actualStart": "2025-12-01T09:05:00",
        "actualEnd": "2025-12-01T12:00:00",
        "workDurationMinutes": 175
      }
    ]
  }
}
```

---

### 9️⃣ 변경 요청 처리

**Endpoint**: `POST /api/v1/admin/schedule/process-change-request`

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "requestIds": [10, 11],
  "statusCode": "CS02"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "변경 요청이 승인되었습니다.",
  "details": null
}
```

---

### 🔟 승인 대기 신청 목록

**Endpoint**: `GET /api/v1/admin/schedule/apply-requests`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 신청 요청 목록을 조회했습니다.",
  "details": {
    "requests": [
      {
        "scheduleId": 123,
        "userId": 1,
        "userName": "홍길동",
        "startTime": "2025-12-01T09:00:00",
        "endTime": "2025-12-01T12:00:00"
      }
    ]
  }
}
```

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [근무 일정 API](./schedule.md)
- [코드 시스템](../database/schema/code-system.md)
