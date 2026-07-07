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

**Base Path**: `/api/attendance`

**주요 기능**:
- QR 토큰 발급 (관리자용, 60초 유효)
- 출근/퇴근 체크 (사용자)
- 출퇴근 이력 조회 (사용자)

---

## 🔐 인증

**QR 토큰 발급** (`GET /api/attendance/qr-token`):
- 인증 없이 호출 가능 (관리자용 QR 토큰 발급용)
- SecurityConfig에서 `permitAll` 설정

**출퇴근 체크/이력 조회** (기타 엔드포인트):
- 방식: `Authorization: Bearer <AccessToken>` 헤더로 JWT AccessToken 전달
- 인증 처리: `@AuthenticationPrincipal CustomUserDetails userDetails` 파라미터로 인증 정보 요청
- Spring Security에서 강제 인증하지 않음 (SecurityConfig에서 `permitAll`)
- **실제 호출 시에는 반드시 AccessToken을 포함**해야 함
- 본인의 출퇴근 이력만 조회 가능 (컨트롤러에서 userId 검증)

---

## 🎫 QR 토큰 발급 (관리자)

### 2.1 QR Token 발급

**Endpoint**: `GET /api/attendance/qr-token`

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

**Endpoint**: `POST /api/attendance/check-in`

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

**Endpoint**: `POST /api/attendance/check-out`

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

**Endpoint**: `GET /api/attendance/today`

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

**Endpoint**: `GET /api/attendance/history`

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
GET /api/attendance/history?date=2026-01-15
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

## 🔧 실전 워크플로우 및 예제

### 시나리오 1️⃣: 완전한 QR 토큰 출퇴근 흐름

**상황**: 사용자가 09:00 ~ 12:00 일정으로 근무 중입니다. 관리자는 QR 토큰을 생성하고 사용자가 스캔하여 출근/퇴근을 처리합니다.

**Step 1: 관리자가 QR 토큰 발급**

```bash
curl -X GET "http://localhost:8080/api/attendance/qr-token" \
  -H "Content-Type: application/json"
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "QR 토큰 발급 성공",
  "details": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxci1hdXRoIiwiaWF0IjoxNjM2MDAwMDAwLCJleHAiOjE2MzYwMDA2MDB9.signature123",
    "expiresAt": "2026-01-22T10:01:00",
    "validSeconds": 60
  }
}
```

관리자는 이 `token`을 QR 코드로 인코딩하여 태블릿 화면에 표시합니다.

**Step 2: 사용자가 09:05에 QR 코드 스캔하여 출근 체크**

사용자가 스마트폰 카메라로 QR 코드를 스캔하면 token을 추출합니다.

```bash
# 출근 체크
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY5Mzk3NDQ2MH0.abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxci1hdXRoIiwiaWF0IjoxNjM2MDAwMDAwLCJleHAiOjE2MzYwMDA2MDB9.signature123"
  }'
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "출근 처리가 완료되었습니다.",
  "details": null
}
```

**TypeScript 프론트엔드 처리 예시**:
```typescript
// QR 스캔 후 토큰 추출
async function handleCheckIn(qrToken: string): Promise<void> {
  try {
    const response = await fetch('/api/attendance/check-in', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ qrToken })
    });

    if (response.ok) {
      // 출근 성공
      showNotification('출근 처리가 완료되었습니다.', 'success');
      updateAttendanceStatus('CHECKED_IN');
    } else if (response.status === 400) {
      const error = await response.json();
      showNotification(error.message, 'error');
    } else if (response.status === 404) {
      showNotification('오늘 근무 일정이 없습니다.', 'error');
    }
  } catch (error) {
    showNotification('출근 처리 중 오류가 발생했습니다.', 'error');
  }
}
```

**Step 3: 사용자가 12:00에 QR 코드 재스캔하여 퇴근 체크**

관리자가 새로운 QR 토큰을 발급하고 사용자가 다시 스캔합니다.

```bash
# 퇴근 체크
curl -X POST "http://localhost:8080/api/attendance/check-out" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY5Mzk3NDQ2MH0.abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxci1hdXRoIiwiaWF0IjoxNjM2MDAxMjAwLCJleHAiOjE2MzYwMDE4MDB9.signature456"
  }'
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "퇴근 처리가 완료되었습니다.",
  "details": null
}
```

**Step 4: 출퇴근 이력 조회**

```bash
curl -X GET "http://localhost:8080/api/attendance/today" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTY5Mzk3NDQ2MH0.abcdef"
```

**Response (200 OK)**:
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

### 시나리오 2️⃣: 검증 실패 시나리오

#### 에러 1: INVALID_QR_TOKEN (토큰 만료)

**상황**: 관리자가 발급한 QR 토큰이 60초를 초과하여 만료되었습니다.

```bash
# 만료된 토큰으로 시도
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxci1hdXRoIiwiaWF0IjoxNjM2MDAwMDAwLCJleHAiOjE2MzYwMDA2MDB9.expired"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "isSuccess": false,
  "message": "QR 토큰이 유효하지 않습니다.",
  "details": null
}
```

**해결책**: 관리자에게 새로운 QR 토큰을 요청하여 다시 스캔합니다. 60초 이내에 처리해야 합니다.

---

#### 에러 2: NO_SCHEDULE_FOUND (일정 없음)

**상황**: 사용자가 오늘 승인된 근무 일정이 없는데 출근을 시도합니다.

```bash
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (404 Not Found)**:
```json
{
  "isSuccess": false,
  "message": "오늘 근무 일정이 없습니다.",
  "details": null
}
```

**해결책**: 근무 일정 API (`/api/work-schedules/apply`)를 통해 일정을 신청하고, 관리자 승인을 받은 후 출근을 시도합니다.

---

#### 에러 3: NOT_WORK_TIME (출근 가능 시간 아님)

**상황**: 근무 일정이 09:00 ~ 12:00인데 08:45에 출근을 시도합니다 (10분 전 윈도우 밖).

```bash
# 08:45에 출근 시도 (09:00 - 10분 = 08:50보다 이름)
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "isSuccess": false,
  "message": "출근 가능 시간이 아닙니다.",
  "details": null
}
```

**시간 윈도우 규칙**:
- **정상 출근 윈도우**: `schedule.start - 10분` ~ `schedule.start`
  - 예: 09:00 일정 → 08:50 ~ 09:00 사이에 출근
- **지각 출근 윈도우**: `schedule.start` ~ `schedule.end`
  - 예: 09:00 ~ 12:00 사이 언제든 출근 가능 (지각으로 기록)

**해결책**: 올바른 시간에 출근을 시도합니다.

```bash
# 08:50 이후에 다시 시도
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

---

#### 에러 4: ALREADY_CHECKED_IN (중복 출근)

**상황**: 사용자가 이미 오늘 해당 일정에 대해 출근했는데 다시 출근을 시도합니다.

```bash
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (409 Conflict)**:
```json
{
  "isSuccess": false,
  "message": "이미 출근 처리되었습니다.",
  "details": null
}
```

**해결책**: 이미 출근이 처리되었으므로 바로 퇴근 또는 이력 조회를 시도합니다.

---

#### 에러 5: CHECK_IN_REQUIRED (출근 없이 퇴근 시도)

**상황**: 사용자가 출근하지 않고 바로 퇴근을 시도합니다.

```bash
curl -X POST "http://localhost:8080/api/attendance/check-out" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "isSuccess": false,
  "message": "출근 체크를 먼저 해주세요.",
  "details": null
}
```

**해결책**: 먼저 출근을 처리한 후에 퇴근을 시도합니다.

---

### 시나리오 3️⃣: 퇴근 시간 윈도우 및 에지 케이스

#### 조퇴 (Early Checkout)

**상황**: 근무 일정이 09:00 ~ 12:00인데 11:50에 퇴근을 시도합니다 (5분 전 윈도우).

```bash
# 11:50에 퇴근 시도 (12:00 - 5분 = 11:55보다 이름)
curl -X POST "http://localhost:8080/api/attendance/check-out" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "isSuccess": false,
  "message": "퇴근 가능 시간이 아닙니다.",
  "details": null
}
```

**퇴근 가능 시간 윈도우**:
- **정상 퇴근**: `schedule.end - 5분` ~ `schedule.end + 1시간`
  - 예: 12:00 일정 → 11:55 ~ 13:00 사이에 퇴근
- 근무 시간은 실제 **출근 시간 ~ 퇴근 시간**으로 계산됨

**올바른 퇴근**:
```bash
# 11:55 이후에 다시 시도
curl -X POST "http://localhost:8080/api/attendance/check-out" \
  -H "Authorization: Bearer <valid_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "qrToken": "<valid_qr_token>"
  }'
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "퇴근 처리가 완료되었습니다.",
  "details": null
}
```

---

#### 지각 출근 + 조퇴 조합

**상황**: 09:00 ~ 12:00 일정에 09:30에 출근했고 11:00에 퇴근합니다 (실제 근무시간: 1시간 30분).

```bash
# 09:30 출근
curl -X POST "http://localhost:8080/api/attendance/check-in" \
  -H "Authorization: Bearer <valid_access_token>" \
  -d '{"qrToken": "<valid_qr_token>"}'
```

응답:
```json
{"isSuccess": true, "message": "출근 처리가 완료되었습니다.", "details": null}
```

```bash
# 11:00 퇴근 (11:55 ~ 13:00 윈도우 내)
curl -X POST "http://localhost:8080/api/attendance/check-out" \
  -H "Authorization: Bearer <valid_access_token>" \
  -d '{"qrToken": "<valid_qr_token>"}'
```

응답:
```json
{"isSuccess": true, "message": "퇴근 처리가 완료되었습니다.", "details": null}
```

이력 조회 결과:
```json
{
  "isSuccess": true,
  "details": {
    "histories": [
      {
        "attendanceId": 125,
        "checkTime": "2026-01-22T09:30:00",
        "checkType": "CT01",
        "scheduleStartTime": "2026-01-22T09:00:00",
        "scheduleEndTime": "2026-01-22T12:00:00"
      },
      {
        "attendanceId": 126,
        "checkTime": "2026-01-22T11:00:00",
        "checkType": "CT02",
        "scheduleStartTime": "2026-01-22T09:00:00",
        "scheduleEndTime": "2026-01-22T12:00:00"
      }
    ]
  }
}
```

**실제 근무시간**: 09:30 ~ 11:00 = **1시간 30분**

---

## ⚠️ 자주 하는 실수 및 해결책

| 실수 | 원인 | 해결책 | 참고 |
|------|------|--------|------|
| **QR 토큰 만료 에러** | 60초 이상 경과 후 스캔 | 새로운 QR 토큰 발급 요청 | 토큰 유효시간: 60초 |
| **오늘 일정 없음 에러** | 근무 일정 미신청 또는 거부 | 근무 일정 API에서 일정 신청 및 승인 | 승인된 일정(WS02)만 가능 |
| **출근 가능 시간 에러** | 스케줄 시작 10분 전보다 이른 시간 | 08:50 ~ 09:00 사이 또는 09:00 이후 출근 | 정상출근: -10분~시작시간, 지각: 시작~종료 |
| **퇴근 불가 에러** | 종료 5분 전이거나 1시간 후 | 11:55 ~ 13:00 윈도우 내에 퇴근 | -5분~+1시간 윈도우 |
| **중복 출근 에러** | 이미 출근한 일정에 재시도 | 이력 확인 후 퇴근 또는 다른 일정 확인 | 출근은 일정당 1회만 가능 |
| **퇴근 선행 조건 에러** | 출근 없이 퇴근 시도 | 먼저 출근을 처리한 후 퇴근 | 퇴근은 출근 이후에만 가능 |
| **토큰 포함 누락** | Authorization 헤더 미포함 | 모든 요청에 `Authorization: Bearer <token>` 추가 | QR 토큰 발급만 토큰 불필요 |

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
