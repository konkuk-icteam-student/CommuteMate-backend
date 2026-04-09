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
- [실전 워크플로우 및 예제](#-실전-워크플로우-및-예제)
  - [월별 근무 환경 설정 및 검증](#scenario-1️⃣-월별-근무-환경-설정-및-검증)
  - [변경 요청 승인/거부 처리](#scenario-2️⃣-변경-요청-승인거부-처리)
  - [월별 근무 시간 통계 및 감시](#scenario-3️⃣-월별-근무-시간-통계-및-감시)
- [자주 하는 실수 및 해결책](#-자주-하는-실수-및-해결책)
- [관련 문서](#-관련-문서)

---

## 📖 개요

관리자용 근무 일정 설정 및 변경 요청 처리 API입니다.

**Base Path**: `/api/admin/schedule`

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

**Endpoint**: `POST /api/admin/schedule/monthly-limit`

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

**Endpoint**: `POST /api/admin/schedule/set-apply-term`

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

**Endpoint**: `GET /api/admin/schedule/monthly-limit/{year}/{month}`

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

**Endpoint**: `GET /api/admin/schedule/monthly-limits`

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

**Endpoint**: `GET /api/admin/schedule/work-time?userId=1&year=2025&month=12`

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

**Endpoint**: `GET /api/admin/schedule/work-time/summary?year=2025&month=12`

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

**Endpoint**: `GET /api/admin/schedule/history?userId=1&year=2025&month=12`

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

**Endpoint**: `GET /api/admin/schedule/history/all?year=2025&month=12`

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

**Endpoint**: `POST /api/admin/schedule/process-change-request`

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

**Endpoint**: `GET /api/admin/schedule/apply-requests`

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

## 🔧 실전 워크플로우 및 예제

### Scenario 1️⃣: 월별 근무 환경 설정 및 검증

**상황**: 2025년 12월의 근무 일정 환경을 새로 세팅하는 경우
- 최대 동시 근무 인원: 15명
- 신청 가능 기간: 11월 15일 09:00 ~ 11월 30일 18:00

**Step 1: 월별 제한 설정**

```bash
curl -X POST "http://localhost:8080/api/admin/schedule/monthly-limit" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "maxConcurrent": 15
  }'
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

**Step 2: 신청 기간 설정**

```bash
curl -X POST "http://localhost:8080/api/admin/schedule/set-apply-term" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "applyStartTime": "2025-11-15T09:00:00",
    "applyEndTime": "2025-11-30T18:00:00"
  }'
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "신청 기간이 설정되었습니다.",
  "details": {
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "applyStartTime": "2025-11-15T09:00:00",
    "applyEndTime": "2025-11-30T18:00:00"
  }
}
```

**Step 3: 설정 검증**

```bash
# 특정 월 설정 확인
curl -X GET "http://localhost:8080/api/admin/schedule/monthly-limit/2025/12" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>"
```

**Step 4: 관리자 대시보드 TypeScript 구현 예시**

```typescript
// Admin Dashboard Component
interface MonthlyConfig {
  scheduleYear: number;
  scheduleMonth: number;
  maxConcurrent: number;
  applyStartTime: string;
  applyEndTime: string;
}

async function setupMonthlySchedule(
  year: number,
  month: number,
  maxConcurrent: number,
  applyStart: string,
  applyEnd: string
): Promise<void> {
  try {
    // Step 1: Set monthly limit
    const limitResponse = await fetch('/api/admin/schedule/monthly-limit', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        scheduleYear: year,
        scheduleMonth: month,
        maxConcurrent
      })
    });

    if (!limitResponse.ok) {
      throw new Error('월별 제한 설정 실패');
    }

    // Step 2: Set apply term
    const termResponse = await fetch('/api/admin/schedule/set-apply-term', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        scheduleYear: year,
        scheduleMonth: month,
        applyStartTime: applyStart,
        applyEndTime: applyEnd
      })
    });

    if (!termResponse.ok) {
      const error = await termResponse.json();
      throw new Error(error.message);
    }

    // Step 3: Verify configuration
    const verifyResponse = await fetch(
      `/api/admin/schedule/monthly-limit/${year}/${month}`,
      {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      }
    );

    const config: MonthlyConfig = (await verifyResponse.json()).details;
    showNotification(
      `${year}년 ${month}월 환경 설정 완료\n` +
      `최대 동시 인원: ${config.maxConcurrent}명\n` +
      `신청 기간: ${new Date(config.applyStartTime).toLocaleDateString()} ~ ` +
      `${new Date(config.applyEndTime).toLocaleDateString()}`,
      'success'
    );
  } catch (error) {
    showNotification(`설정 실패: ${error.message}`, 'error');
  }
}
```

---

### Scenario 2️⃣: 변경 요청 승인/거부 처리

**상황**: 사용자가 제출한 근무 변경 요청(수정/삭제)을 일괄 처리하는 경우

**Step 1: 승인 대기 목록 조회**

```bash
curl -X GET "http://localhost:8080/api/admin/schedule/apply-requests" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>"
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 신청 요청 목록을 조회했습니다.",
  "details": {
    "requests": [
      {
        "scheduleId": 123,
        "userId": 5,
        "userName": "김철수",
        "startTime": "2025-12-01T09:00:00",
        "endTime": "2025-12-01T13:00:00"
      },
      {
        "scheduleId": 124,
        "userId": 8,
        "userName": "이순신",
        "startTime": "2025-12-02T14:00:00",
        "endTime": "2025-12-02T18:00:00"
      }
    ]
  }
}
```

**Step 2: 변경 요청 승인 처리**

```bash
# 요청 ID 123, 124를 승인 (CS02 = APPROVED)
curl -X POST "http://localhost:8080/api/admin/schedule/process-change-request" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestIds": [123, 124],
    "statusCode": "CS02"
  }'
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "변경 요청이 승인되었습니다.",
  "details": null
}
```

**Step 3: 특정 요청 거부 처리**

```bash
# 요청 ID 125를 거부 (CS03 = REJECTED)
curl -X POST "http://localhost:8080/api/admin/schedule/process-change-request" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestIds": [125],
    "statusCode": "CS03"
  }'
```

**Step 4: TypeScript 변경 요청 처리 로직**

```typescript
interface ChangeRequest {
  scheduleId: number;
  userId: number;
  userName: string;
  startTime: string;
  endTime: string;
}

async function processChangeRequests(
  requestIds: number[],
  decision: 'APPROVE' | 'REJECT'
): Promise<void> {
  const statusCode = decision === 'APPROVE' ? 'CS02' : 'CS03';

  try {
    const response = await fetch('/api/admin/schedule/process-change-request', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requestIds,
        statusCode
      })
    });

    if (response.ok) {
      const message = decision === 'APPROVE'
        ? `${requestIds.length}개의 요청이 승인되었습니다.`
        : `${requestIds.length}개의 요청이 거부되었습니다.`;
      showNotification(message, 'success');
      await refreshPendingRequests();
    } else {
      throw new Error('변경 요청 처리 실패');
    }
  } catch (error) {
    showNotification(`처리 실패: ${error.message}`, 'error');
  }
}
```

---

### Scenario 3️⃣: 월별 근무 시간 통계 및 감시

**상황**: 12월 전체 근무 통계를 조회하고 특정 사용자의 근무 현황을 파악하는 경우

**Step 1: 전체 근무 시간 통계 조회**

```bash
curl -X GET "http://localhost:8080/api/admin/schedule/work-time/summary?year=2025&month=12" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>"
```

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
          "email": "hong@example.com",
          "name": "홍길동",
          "role": "RL01",
          "organizationId": 1
        },
        "totalMinutes": 1320
      },
      {
        "userInfo": {
          "userId": 5,
          "email": "kim@example.com",
          "name": "김철수",
          "role": "RL01",
          "organizationId": 1
        },
        "totalMinutes": 960
      }
    ]
  }
}
```

**Step 2: 특정 사용자 근무 이력 상세 조회**

```bash
# 사용자 ID 1의 12월 상세 이력
curl -X GET "http://localhost:8080/api/admin/schedule/history?userId=1&year=2025&month=12" \
  -H "Authorization: Bearer <JWT_ADMIN_TOKEN>"
```

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
      },
      {
        "id": 124,
        "start": "2025-12-02T14:00:00",
        "end": "2025-12-02T18:00:00",
        "status": "WS02",
        "actualStart": "2025-12-02T14:00:00",
        "actualEnd": "2025-12-02T17:55:00",
        "workDurationMinutes": 235
      }
    ]
  }
}
```

**Step 3: 통계 분석 TypeScript 코드**

```typescript
interface WorkStatistics {
  userId: number;
  userName: string;
  totalMinutes: number;
  expectedMinutes: number;
  attendance: number;
}

async function analyzeMonthlyStatistics(year: number, month: number): Promise<void> {
  try {
    // Get summary
    const summaryResponse = await fetch(
      `/api/admin/schedule/work-time/summary?year=${year}&month=${month}`,
      { headers: { 'Authorization': `Bearer ${adminToken}` } }
    );

    const summaryData = await summaryResponse.json();
    const statistics: WorkStatistics[] = [];

    // Process each user
    for (const user of summaryData.details.summary) {
      const hoursWorked = Math.round(user.totalMinutes / 60 * 10) / 10;
      const expectedHours = 160; // Standard monthly hours

      statistics.push({
        userId: user.userInfo.userId,
        userName: user.userInfo.name,
        totalMinutes: user.totalMinutes,
        expectedMinutes: expectedHours * 60,
        attendance: Math.round((user.totalMinutes / (expectedHours * 60)) * 100)
      });
    }

    // Display dashboard
    displayStatisticsDashboard(statistics);

    // Alert for low attendance
    statistics.forEach(stat => {
      if (stat.attendance < 80) {
        console.warn(
          `⚠️ ${stat.userName}: 출근율 ${stat.attendance}% ` +
          `(${stat.totalMinutes}분 / ${stat.expectedMinutes}분)`
        );
      }
    });
  } catch (error) {
    console.error('통계 분석 실패:', error.message);
  }
}
```

---

## ⚠️ 자주 하는 실수 및 해결책

| 실수 | 원인 | HTTP 상태 | 해결책 | 참고 |
|------|------|----------|--------|------|
| **신청 기간 설정 실패** | applyStartTime >= applyEndTime | 422 | 시작 시간이 종료 시간보다 먼저인지 확인. 예: 11/15 09:00 < 11/30 18:00 | 유효성 검증: start < end |
| **월별 제한 없이 신청 기간만 설정** | 월별 제한(maxConcurrent)이 먼저 필요 | 404 | 1) 월별 제한 설정 POST 2) 신청 기간 설정 POST. 순서 중요 | 의존성: 제한 → 기간 |
| **타임스탬프 형식 오류** | ISO 8601 형식이 아님 (예: "2025-12-01 09:00") | 400 | 항상 ISO 8601 형식 사용: "2025-12-01T09:00:00" | 형식: YYYY-MM-DDTHH:mm:ss |
| **존재하지 않는 변경 요청 처리** | requestIds가 유효하지 않음 | 404 | 먼저 GET /apply-requests로 유효한 ID 조회 후 처리 | 검증: 목록 확인 → 처리 |
| **Authorization 헤더 누락** | 인증 정보 없이 요청 | 401 | 모든 POST/GET 요청에 Authorization 헤더 포함: `Bearer <TOKEN>` | 필수: 헤더 추가 |
| **없는 월 데이터 조회** | 설정하지 않은 월 조회 | 404 | 먼저 GET /monthly-limits로 설정된 월 목록 확인 후 조회 | 확인: 전체 목록 → 조회 |
| **잘못된 상태 코드** | statusCode가 유효하지 않은 값 (예: "TEST") | 400 | CS02 (승인) 또는 CS03 (거부)만 사용. CodeType 참고 | 유효값: CS02, CS03 |
| **예상 이상의 통계 결과** | 실제 근무 시간(actualStart/End) vs 스케줄(start/end) 혼동 | - | workDurationMinutes는 actualStart ~ actualEnd 기반. 지각/조퇴 반영됨 | 계산: 실제 시간 기준 |

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [근무 일정 API](./schedule.md)
- [코드 시스템](../database/schema/code-system.md)

