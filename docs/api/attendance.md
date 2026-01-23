# 출퇴근 API (Attendance)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [QR 토큰 발급 (관리자)](#-qr-토큰-발급-관리자)
- [출근 체크](#-출근-체크)
- [퇴근 체크](#-퇴근-체크)
- [출퇴근 이력 조회](#-출퇴근-이력-조회)
- [관련 문서](#-관련-문서)

---

## 📖 개요

QR 코드를 이용한 출퇴근 인증 및 이력 관리를 담당하는 API입니다.
관리자가 발급한 QR 토큰을 스캔하여 출퇴근을 체크하고, 이력을 조회할 수 있습니다.

**Base Path**: `/api/v1/attendance`

**주요 기능**:
- QR 토큰 발급 (관리자용, 60초 유효)
- 출근/퇴근 체크 (사용자)
- 출퇴근 이력 조회 (사용자)

---

## 🔐 인증

- **QR 토큰 발급**: 인증 없이 호출 가능 (관리자용으로 사용 권장)
- **출퇴근 체크/이력 조회**: `Authorization: Bearer <AccessToken>` 필요

---

## 🎫 QR 토큰 발급 (관리자)

### 2.1 QR Token 발급

**Endpoint**: `GET /api/v1/attendance/qr-token`

**설명**: 관리자 태블릿 등에서 띄울 QR 코드용 토큰을 생성합니다. 토큰은 60초간 유효합니다.

**권한**: 관리자용으로 사용 (권한 검증 미적용)

#### Request

**Headers**: 없음

**Parameters**: 없음

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "QR 토큰 발급 성공",
  "details": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresAt": "2026-01-22T10:01:00",
    "validSeconds": 60
  }
}
```

#### 사용 시나리오

1. 관리자 태블릿이 1분마다 이 API를 호출
2. 받은 `token`을 QR 코드로 변환하여 화면에 표시
3. 사용자가 스마트폰으로 QR 코드를 스캔하여 토큰 획득
4. 획득한 토큰으로 출근/퇴근 체크

---

## ✅ 출근 체크

### 3.1 Check-In

**Endpoint**: `POST /api/v1/attendance/check-in`

**설명**: QR 코드를 스캔하여 출근 체크를 수행합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "출근 처리가 완료되었습니다.",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `INVALID_QR_TOKEN` | QR 토큰이 유효하지 않습니다. | 토큰 만료 또는 잘못된 토큰 |
| 404 | `NO_SCHEDULE_FOUND` | 오늘 근무 일정이 없습니다. | 해당 날짜에 승인된 스케줄 없음 |
| 400 | `NOT_WORK_TIME` | 출근 가능 시간이 아닙니다. | 스케줄 시작 10분 전보다 이른 시간 |
| 409 | `ALREADY_CHECKED_IN` | 이미 출근 처리되었습니다. | 중복 체크인 시도 |

#### 출근 가능 시간

**정상 출근**: `schedule.start - 10분` ~ `schedule.start`
**지각 출근**: `schedule.start` ~ `schedule.end`

#### 처리 로직

1. QR 토큰 유효성 검증 (Redis/Memory)
2. 사용자의 오늘 스케줄 조회
3. **출근 가능 시간 검증**: `now`가 `schedule.start - 10분` ~ `schedule.end` 사이여야 함
4. 중복 체크인 검증 (이미 `CT01` 기록이 있으면 예외)
5. `WorkAttendance` 생성 (타입 `CT01`)

#### Edge Cases

| 상황 | 처리 방식 |
|------|----------|
| **QR 만료** | 60초 지나면 `INVALID_QR_TOKEN` 에러 |
| **너무 이른 출근** | 스케줄 시작 10분 전보다 이르면 체크인 불가 → `NOT_WORK_TIME` |
| **스케줄 없음** | `NO_SCHEDULE_FOUND` 에러 |
| **지각** | 시작 시간 이후에도 출근 가능, 실제 찍은 시간으로 기록 |
| **연속 스케줄** | 가장 가까운 스케줄에 대해 출근 처리 |

---

## 🏁 퇴근 체크

### 3.2 Check-Out

**Endpoint**: `POST /api/v1/attendance/check-out`

**설명**: QR 코드를 스캔하여 퇴근 체크를 수행합니다. **출근 체크가 선행되어야 합니다.**

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "퇴근 처리가 완료되었습니다.",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `INVALID_QR_TOKEN` | QR 토큰이 유효하지 않습니다. | 토큰 만료 또는 잘못된 토큰 |
| 404 | `NO_SCHEDULE_FOUND` | 오늘 근무 일정이 없습니다. | 해당 날짜에 승인된 스케줄 없음 |
| 400 | `CHECK_IN_REQUIRED` | 출근 체크를 먼저 해주세요. | 출근 기록 없이 퇴근 시도 |
| 400 | `NOT_WORK_TIME` | 퇴근 가능 시간이 아닙니다. | 퇴근 가능 시간 범위 밖 |
| 409 | `ALREADY_CHECKED_OUT` | 이미 퇴근 처리되었습니다. | 중복 체크아웃 시도 |

#### 퇴근 가능 시간

**정상 퇴근**: `schedule.end - 5분` ~ `schedule.end + 1시간`

#### 처리 로직

1. QR 토큰 유효성 검증
2. 사용자의 오늘 스케줄 조회
3. **퇴근 가능 시간 검증**: `now`가 `schedule.end - 5분` ~ `schedule.end + 1시간` 사이여야 함
4. **선행 조건 검증**: 해당 스케줄에 `Check-In` 기록이 반드시 있어야 함
5. 중복 체크아웃 검증 (이미 `CT02` 기록이 있으면 예외)
6. `WorkAttendance` 생성 (타입 `CT02`)

#### Edge Cases

| 상황 | 처리 방식 |
|------|----------|
| **출근 안하고 퇴근 시도** | `CHECK_IN_REQUIRED` 에러 |
| **너무 이른 퇴근** | 종료 5분 전보다 이르면 퇴근 불가 → `NOT_WORK_TIME` |
| **너무 늦은 퇴근** | 종료 후 1시간 지나면 퇴근 불가 → `NOT_WORK_TIME` |
| **조퇴** | 종료 5분 전부터 퇴근 가능, 실제 근무 시간은 출근~퇴근 시간으로 계산 |
| **연속 스케줄** | 출근한 스케줄에 대해서만 퇴근 가능 |

---

## 📋 출퇴근 이력 조회

### 4.1 오늘의 출퇴근 기록 조회

**Endpoint**: `GET /api/v1/attendance/today`

**설명**: 오늘 날짜의 출퇴근 이력 리스트를 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Parameters**: 없음

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "오늘의 출퇴근 기록 조회 성공",
  "details": {
    "histories": [
      {
        "attendanceId": 123,
        "scheduleId": 456,
        "checkTime": "2026-01-22T09:05:00",
        "checkType": "CT01",
        "scheduleStartTime": "2026-01-22T09:00:00",
        "scheduleEndTime": "2026-01-22T12:00:00"
      },
      {
        "attendanceId": 124,
        "scheduleId": 456,
        "checkTime": "2026-01-22T12:00:00",
        "checkType": "CT02",
        "scheduleStartTime": "2026-01-22T09:00:00",
        "scheduleEndTime": "2026-01-22T12:00:00"
      }
    ]
  }
}
```

---

### 4.2 특정 날짜 출퇴근 기록 조회

**Endpoint**: `GET /api/v1/attendance/history`

**설명**: 특정 날짜의 출퇴근 이력 리스트를 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `date` | String | Yes | 조회할 날짜 `yyyy-MM-dd` |

**Example**:
```
GET /api/v1/attendance/history?date=2026-01-15
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "출퇴근 이력 조회 성공",
  "details": {
    "histories": [
      {
        "attendanceId": 100,
        "scheduleId": 400,
        "checkTime": "2026-01-15T10:00:00",
        "checkType": "CT01",
        "scheduleStartTime": "2026-01-15T10:00:00",
        "scheduleEndTime": "2026-01-15T13:00:00"
      },
      {
        "attendanceId": 101,
        "scheduleId": 400,
        "checkTime": "2026-01-15T13:00:00",
        "checkType": "CT02",
        "scheduleStartTime": "2026-01-15T10:00:00",
        "scheduleEndTime": "2026-01-15T13:00:00"
      }
    ]
  }
}
```

**Error Responses**:

| Status | Message | 설명 |
|--------|---------|------|
| 400 | 입력값이 올바르지 않습니다. | `date` 형식이 `yyyy-MM-dd`가 아닌 경우 |

---

## 🔗 관련 문서

### 연관 API
- [대시보드 API](./home.md) - 출퇴근 상태 조회
- [근무 일정 API](./schedule.md) - 일정 신청 및 조회
- [인증 API](./auth.md) - 로그인 및 권한 관리

### 규약 및 시스템
- [에러 처리 규약](../conventions/error-handling.md)
- [API 설계 규약](../conventions/api-conventions.md)
- [코드 시스템](../database/schema/code-system.md) - CheckType(`CT`)

### 데이터베이스
- [WorkAttendance 스키마](../database/README.md) - 출퇴근 기록 테이블

### 상위 문서
- ⬆️ [API 문서 홈](./README.md)
- ⬆️ [문서 허브](../README.md)
