# 홈 화면 API (Home Dashboard)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [주요 엔드포인트](#-주요-엔드포인트)
- [상세 엔드포인트](#-상세-엔드포인트)
  - [오늘의 근무 시간 조회](#1️⃣-오늘의-근무-시간-조회)
  - [출퇴근 상태 조회](#2️⃣-출퇴근-상태-조회)
  - [주간/월간 근무 시간 요약 조회](#3️⃣-주간월간-근무-시간-요약-조회)
  - [내 정보 조회](#4️⃣-내-정보-조회)
  - [주간 근무 시간 조회](#5️⃣-주간-근무-시간-조회)
  - [월간 근무 시간 조회](#6️⃣-월간-근무-시간-조회)
- [응답 구조](#-응답-구조)
- [사용 예시](#-사용-예시)
- [주의사항](#-주의사항)
- [관련 문서](#-관련-문서)

---

## 📖 개요

홈 화면(대시보드)에 필요한 정보를 조회하는 API입니다. 사용자의 오늘 근무 현황, 출퇴근 상태, 주간/월간 근무 시간 요약 정보를 제공합니다.

**Base Path**: `/api/v1/home`, `/api/v1/users`

**주요 기능**:
- 오늘의 누적 근무 시간 및 스케줄 개수 조회
- 현재 시간 기반 출퇴근 버튼 상태 판별
- 주간/월간 근무 시간 통계 조회
- 실시간 근무 상태 추적

**시간 계산 기준**:
- 실제 출퇴근 체크 시간 기반 계산
- 부분 근무 시간도 정확히 집계
- 시간 단위: 분 단위 또는 시간 단위(소수 가능)

---

## 🔐 인증

모든 엔드포인트는 인증 사용자 정보를 사용하므로 **JWT AccessToken**이 필요합니다.

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**요구 사항**:
- 본인의 정보만 조회 가능
- 유효한 AccessToken 필수
- 인증 정보 없이 호출 시 오류가 발생할 수 있음

---

## 📝 주요 엔드포인트

| 메서드 | 경로 | 설명 | 응답 상태 |
|--------|------|------|---------|
| GET | `/work-time` | 오늘의 근무 시간 조회 | 200 |
| GET | `/attendance-status` | 출퇴근 상태 조회 | 200 |
| GET | `/work-summary` | 주간/월간 근무 시간 요약 조회 | 200 |
| GET | `/api/v1/users/me` | 내 정보 조회 | 200 |
| GET | `/api/v1/users/me/work-time/weekly` | 주간 근무 시간 조회 | 200 |
| GET | `/api/v1/users/me/work-time/monthly` | 월간 근무 시간 조회 | 200 |

---

## 📋 상세 엔드포인트

### 1️⃣ 오늘의 근무 시간 조회

**Endpoint**: `GET /api/v1/home/work-time`

**설명**: 현재 사용자의 오늘 누적 근무 시간(분)과 예정된 스케줄 개수를 조회합니다.

**포함 정보**:
- 총 근무 시간 (분 단위): 실제 출퇴근 체크 기록 기반 계산
- 스케줄 개수: 오늘 유효한 근무 일정(WS01/WS02)의 개수
- 진행 중인 근무도 포함 (퇴근 체크 전까지)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Query Parameters**: 없음

**Body**: 없음

#### Response

**Success - 200 OK**

```json
{
  "isSuccess": true,
  "message": "오늘의 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 240,
    "scheduleCount": 2
  }
}
```

**필드 설명**:
- `totalMinutes` (long): 오늘 누적 근무 시간 (분 단위)
  - 예: 240분 = 4시간, 120분 = 2시간
  - 출퇴근 체크 기록이 없으면 0
  - 진행 중인 근무는 현재 시각까지 포함
- `scheduleCount` (int): 오늘 예정된 근무 스케줄 개수

**Error - 404 Not Found**
```json
{
  "isSuccess": false,
  "message": "사용자를 찾을 수 없습니다.",
  "details": null
}
```

#### 근무 시간 계산 로직

1. **오늘(00:00 ~ 23:59)의 모든 유효한 근무 스케줄(WS01/WS02) 조회**
2. **각 스케줄별 출퇴근 기록 확인**:
   - 출근 체크(CT01) 시간
   - 퇴근 체크(CT02) 시간 (없으면 현재 시각)
3. **근무 시간 계산**:
   - 출근 시간과 퇴근 시간 사이의 실제 경과 시간
   - 스케줄 시간대를 벗어나면 보정
4. **전체 합산**: 모든 스케줄의 근무 시간 합계

#### 계산 예시

```
스케줄 1: 09:00 ~ 12:00 (3시간)
  ├─ 출근: 08:55 → 09:00로 보정
  └─ 퇴근: 12:10 → 12:00으로 보정
  └─ 실제 근무: 3시간 (180분)

스케줄 2: 14:00 ~ 17:00 (3시간)
  ├─ 출근: 14:05 (미기록)
  └─ 현재: 16:10
  └─ 실제 근무: 2시간 5분 (125분, 진행 중)

총 근무 시간: 305분 (5시간 5분)
스케줄 개수: 2
```

---

### 2️⃣ 출퇴근 상태 조회

**Endpoint**: `GET /api/v1/home/attendance-status`

**설명**: 현재 시간 기반으로 사용자의 출퇴근 버튼 상태를 실시간으로 조회합니다.

**포함 정보**:
- 현재 상태 (출근 전, 출근 가능, 근무 중, 퇴근 가능 등)
- 현황 메시지
- 현재 관련 스케줄 정보

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Query Parameters**: 없음

**Body**: 없음

#### Response

**Success - 200 OK (스케줄 없음)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "NO_SCHEDULE",
    "message": "오늘 예정된 근무가 없습니다.",
    "currentScheduleId": null,
    "scheduleStartTime": null,
    "scheduleEndTime": null
  }
}
```
※ 퇴근 체크가 완료된 경우에는 `COMPLETED` 상태라도 스케줄 정보가 포함될 수 있습니다.

**Success - 200 OK (출근 전 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "BEFORE_WORK",
    "message": "출근 전입니다.",
    "currentScheduleId": 123,
    "scheduleStartTime": "2025-01-23T09:00:00",
    "scheduleEndTime": "2025-01-23T12:00:00"
  }
}
```

**Success - 200 OK (출근 가능 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "CAN_CHECK_IN",
    "message": "출근 체크가 가능합니다.",
    "currentScheduleId": 123,
    "scheduleStartTime": "2025-01-23T09:00:00",
    "scheduleEndTime": "2025-01-23T12:00:00"
  }
}
```

**Success - 200 OK (근무 중 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "WORKING",
    "message": "근무 중입니다.",
    "currentScheduleId": 123,
    "scheduleStartTime": "2025-01-23T09:00:00",
    "scheduleEndTime": "2025-01-23T12:00:00"
  }
}
```

**Success - 200 OK (지각 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "LATE_CHECK_IN",
    "message": "지각입니다. 서둘러 출근하세요!",
    "currentScheduleId": 123,
    "scheduleStartTime": "2025-01-23T09:00:00",
    "scheduleEndTime": "2025-01-23T12:00:00"
  }
}
```

**Success - 200 OK (퇴근 가능 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "CAN_CHECK_OUT",
    "message": "퇴근 체크가 가능합니다.",
    "currentScheduleId": 123,
    "scheduleStartTime": "2025-01-23T09:00:00",
    "scheduleEndTime": "2025-01-23T12:00:00"
  }
}
```

**Success - 200 OK (근무 완료 상태)**

```json
{
  "isSuccess": true,
  "message": "출퇴근 상태 조회 성공",
  "details": {
    "status": "COMPLETED",
    "message": "근무가 종료되었습니다.",
    "currentScheduleId": null,
    "scheduleStartTime": null,
    "scheduleEndTime": null
  }
}
```

**필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | string | 출퇴근 상태 코드 (Enum) |
| `message` | string | 상태에 대한 설명 메시지 |
| `currentScheduleId` | integer | 현재 관련된 스케줄 ID (상태 없으면 null) |
| `scheduleStartTime` | datetime | 스케줄 시작 시간 (ISO 8601 형식) |
| `scheduleEndTime` | datetime | 스케줄 종료 시간 (ISO 8601 형식) |

#### 상태 코드 설명

| 상태 | 코드 | 설명 | 시나리오 |
|------|------|------|---------|
| NO_SCHEDULE | `NO_SCHEDULE` | 예정된 근무 없음 | 오늘 유효한 스케줄이 없음 |
| 출근 전 | `BEFORE_WORK` | 출근 전 상태 | 현재 시각이 스케줄 시작 10분 이전 |
| 출근 가능 | `CAN_CHECK_IN` | 출근 가능 | 스케줄 시작 10분 전부터 시작 시각까지 |
| 근무 중 | `WORKING` | 근무 중 | 출근 후 퇴근 5분 전까지 |
| 퇴근 가능 | `CAN_CHECK_OUT` | 퇴근 가능 | 스케줄 종료 5분 전부터 종료 후 1시간까지 |
| 지각 | `LATE_CHECK_IN` | 지각 상태 | 스케줄 시작 시각 지났는데 출근 미체크 |
| 완료 | `COMPLETED` | 근무 완료 | 모든 스케줄 퇴근 완료 또는 1시간 이상 경과 |

#### 상태 전이 다이어그램

```
NO_SCHEDULE (스케줄 없음)
  ↓ (스케줄 존재)

BEFORE_WORK (시작 10분 전)
  ↓ (시간 경과)

CAN_CHECK_IN (시작 10분 ~ 정각)
  ↓ (출근 체크)

WORKING (퇴근 5분 전까지)
  ↓ (시간 경과, 퇴근 5분 전)

CAN_CHECK_OUT (퇴근 가능)
  ↓ (퇴근 체크)

COMPLETED (완료)

또는:
CAN_CHECK_IN (출근 미체크 상태 지속)
  ↓ (스케줄 시작 시각 경과)

LATE_CHECK_IN (지각 상태)
  ↓ (출근 체크 또는 시간 경과)

...
```

#### 특수 상황 처리

**상황 1: 여러 스케줄이 있는 경우**
- 현재 시각과 가장 연관성 있는 스케줄을 자동으로 선택
- 우선순위:
  1. 진행 중인 스케줄 (10분 전 ~ 1시간 후)
  2. 다음 스케줄 (현재 시각 이후)
  3. 마지막 스케줄 (모든 스케줄 종료 후)

**상황 2: 스케줄 종료 후**
- 종료 1시간 이내: 마지막 스케줄 정보 표시
- 종료 1시간 이상 경과: `COMPLETED` 상태, 스케줄 정보 없음

**Error - 404 Not Found**
```json
{
  "isSuccess": false,
  "message": "사용자를 찾을 수 없습니다.",
  "details": null
}
```

---

### 3️⃣ 주간/월간 근무 시간 요약 조회

**Endpoint**: `GET /api/v1/home/work-summary`

**설명**: 이번 주 및 이번 달의 근무 시간 통계를 조회합니다.

**포함 정보**:
- 이번 주 전체 예정 근무 시간
- 이번 주 완료된 근무 시간 (퇴근 체크 완료분만)
- 이번 달 완료된 근무 시간 (퇴근 체크 완료분만)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Query Parameters**: 없음

**Body**: 없음

#### Response

**Success - 200 OK**

```json
{
  "isSuccess": true,
  "message": "근무 시간 요약 조회 성공",
  "details": {
    "totalWeeklyHours": 20.0,
    "completedWeeklyHours": 12.5,
    "completedMonthlyHours": 35.5
  }
}
```

**필드 설명**:

| 필드 | 타입 | 설명 | 단위 |
|------|------|------|------|
| `totalWeeklyHours` | double | 이번 주 전체 근무 시간 (유효한 스케줄 기준) | 시간 (소수) |
| `completedWeeklyHours` | double | 이번 주 완료된 근무 시간 (퇴근 체크된 스케줄만) | 시간 (소수) |
| `completedMonthlyHours` | double | 이번 달 완료된 근무 시간 (퇴근 체크된 스케줄만) | 시간 (소수) |

**단위 설명**:
- 시간 단위로 표시 (소수 가능)
- 예: 3.0 = 3시간, 3.25 = 3시간 15분, 4.0 = 4시간

#### 기간 정의

**이번 주**:
- 월요일 00:00 ~ 일요일 23:59
- 예: 현재가 목요일이면 같은 주 월요일부터 일요일까지

**이번 달**:
- 1일 00:00 ~ 말일 23:59
- 예: 1월이면 1월 1일 ~ 1월 31일

#### 계산 로직

1. **전체 근무 시간 (totalWeeklyHours)**:
   - 이번 주의 모든 유효한 근무 스케줄(WS01/WS02)의 예정 시간
   - 유효한 스케줄(WS01/WS02)만 포함
   - 계산식: (종료 시간 - 시작 시간)의 합계

2. **완료된 근무 시간 (completedWeeklyHours, completedMonthlyHours)**:
   - 퇴근 체크(CT02) 기록이 있는 스케줄만 포함
   - 실제 출퇴근 시간이 아닌 스케줄의 예정 시간 기반
   - 계산식: 퇴근 체크된 스케줄의 (종료 시간 - 시작 시간) 합계

#### 계산 예시

```
[이번 주 스케줄]
월: 09:00 ~ 12:00 (3시간) → 퇴근 체크 완료 ✓
화: 14:00 ~ 17:00 (3시간) → 퇴근 체크 완료 ✓
수: 09:00 ~ 12:30 (3.5시간) → 퇴근 체크 미완료 ✗
목: 15:00 ~ 18:00 (3시간) → 퇴근 체크 완료 ✓
금: (예정된 근무 없음)

[계산 결과]
totalWeeklyHours: 3 + 3 + 3.5 + 3 = 12.5시간
completedWeeklyHours: 3 + 3 + 3 = 9.0시간

[이번 달 (1월) 완료 스케줄]
- 지난주 금: 3시간 (퇴근 완료)
- 지난주 토: 2.5시간 (퇴근 완료)
- 월: 3시간 (퇴근 완료)
- 화: 3시간 (퇴근 완료)
- 목: 3시간 (퇴근 완료)
+ 기타...

completedMonthlyHours: 25.5시간 (예시)
```

**Error - 404 Not Found**
```json
{
  "isSuccess": false,
  "message": "사용자를 찾을 수 없습니다.",
  "details": null
}
```

---

### 4️⃣ 내 정보 조회

**Endpoint**: `GET /api/v1/users/me`

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

#### Response

**Success - 200 OK**
```json
{
  "isSuccess": true,
  "message": "내 정보 조회 성공",
  "details": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "RL01",
    "organizationId": 1
  }
}
```

---

### 5️⃣ 주간 근무 시간 조회

**Endpoint**: `GET /api/v1/users/me/work-time/weekly`

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

#### Response

**Success - 200 OK**
```json
{
  "isSuccess": true,
  "message": "주간 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 480,
    "periodType": "WEEKLY"
  }
}
```

---

### 6️⃣ 월간 근무 시간 조회

**Endpoint**: `GET /api/v1/users/me/work-time/monthly`

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

#### Response

**Success - 200 OK**
```json
{
  "isSuccess": true,
  "message": "월간 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 1620,
    "periodType": "MONTHLY"
  }
}
```

---

## 🎯 응답 구조

모든 응답은 다음의 일관된 구조를 따릅니다:

### 기본 응답 형식

```json
{
  "isSuccess": boolean,
  "message": "string",
  "details": object | null
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `isSuccess` | boolean | 요청 성공 여부 (true/false) |
| `message` | string | 성공/실패 메시지 |
| `details` | object \| null | 응답 데이터 (실패 시 null) |

### HTTP 상태 코드

| 상태 | 코드 | 설명 |
|------|------|------|
| 성공 | 200 OK | 요청이 정상 처리됨 |
| 인증 실패 | 401 Unauthorized | AccessToken 누락 또는 유효하지 않음 |
| 사용자 미존재 | 404 Not Found | 해당 사용자를 찾을 수 없음 |
| 서버 오류 | 500 Internal Server Error | 서버 처리 중 오류 발생 |

---

## 💡 사용 예시

### 예시 1: 홈 화면 초기 로딩

사용자가 앱을 켰을 때 필요한 정보 조회:

```bash
# 1. 오늘의 근무 시간 조회
curl -X GET "https://api.commutemate.com/api/v1/home/work-time" \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json"

# 2. 출퇴근 상태 조회
curl -X GET "https://api.commutemate.com/api/v1/home/attendance-status" \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json"

# 3. 주간/월간 요약 조회
curl -X GET "https://api.commutemate.com/api/v1/home/work-summary" \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json"
```

### 예시 2: 출근 시간대 (아침 8:50)

```javascript
// JavaScript 예시
async function checkMorningStatus() {
  try {
    const response = await fetch('/api/v1/home/attendance-status', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.isSuccess) {
      const status = data.details.status;

      if (status === 'CAN_CHECK_IN') {
        // UI: 출근 버튼 활성화
        showCheckInButton(data.details);
      } else if (status === 'BEFORE_WORK') {
        // UI: "10분 후 출근 가능" 메시지 표시
        showCountdown();
      }
    }
  } catch (error) {
    console.error('상태 조회 실패:', error);
  }
}
```

### 예시 3: 점심 시간 (낮 12:30)

```javascript
// 근무 중 상태 조회
async function checkLunchTime() {
  try {
    const response = await fetch('/api/v1/home/attendance-status', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();
    const status = data.details.status;

    if (status === 'WORKING') {
      // UI: "근무 중"
      showWorkingUI();
    } else if (status === 'CAN_CHECK_OUT') {
      // UI: 퇴근 버튼 활성화
      showCheckOutButton();
    }
  } catch (error) {
    console.error('상태 조회 실패:', error);
  }
}
```

### 예시 4: 실시간 근무 시간 업데이트 (매 분)

```javascript
// 매 분마다 근무 시간 갱신
setInterval(async () => {
  try {
    const response = await fetch('/api/v1/home/work-time', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.isSuccess) {
      const totalHours = data.details.totalMinutes / 60;
      const totalMinutes = data.details.totalMinutes % 60;

      // UI 업데이트
      document.getElementById('workTime').textContent =
        `${Math.floor(totalHours)}:${String(totalMinutes).padStart(2, '0')}`;
    }
  } catch (error) {
    console.error('시간 조회 실패:', error);
  }
}, 60000); // 60초마다 갱신
```

### 예시 5: 주간 근무 현황 확인 (목요일)

```javascript
// 주간 요약 정보 조회
async function showWeeklySummary() {
  try {
    const response = await fetch('/api/v1/home/work-summary', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.isSuccess) {
      const summary = data.details;

      // 진행률 계산
      const weeklyProgress = (summary.completedWeeklyHours / summary.totalWeeklyHours) * 100;

      console.log(`이번 주 진행률: ${weeklyProgress.toFixed(1)}%`);
      console.log(`완료: ${summary.completedWeeklyHours}시간 / 예정: ${summary.totalWeeklyHours}시간`);
      console.log(`이번 달 완료: ${summary.completedMonthlyHours}시간`);

      // UI 업데이트
      updateProgressBar(weeklyProgress);
    }
  } catch (error) {
    console.error('요약 조회 실패:', error);
  }
}
```

---

## ⚠️ 주의사항

### 시간 계산 관련

1. **출퇴근 체크 필수**:
   - 근무 시간은 실제 출퇴근 체크 기록 기반으로 계산됩니다
   - 체크 없으면 근무 시간으로 계산되지 않습니다

2. **완료 여부 판단**:
   - 퇴근 체크(CT02) 기록이 있어야만 "완료"로 간주됩니다
   - 출근 체크만 있고 퇴근 체크가 없으면 미완료입니다

3. **시간 단위 변환**:
   - `/work-time`: 분 단위 (예: 240분)
   - `/work-summary`: 시간 단위 (예: 4.0시간, 4.5시간)
   - 필요시 변환: 분 ÷ 60 = 시간

### 상태 조회 관련

1. **실시간 업데이트**:
   - 상태는 호출 시점의 현재 시각 기반으로 판별됩니다
   - 캐시 사용 시 주기적으로 다시 조회하세요

2. **여러 스케줄 처리**:
   - 하루에 여러 스케줄이 있을 수 있습니다
   - API는 가장 관련성 높은 스케줄을 자동으로 선택합니다

3. **타임존 고려**:
   - 모든 시간은 서버의 로컬 타임존 기준입니다
   - 클라이언트의 타임존과 차이가 있을 수 있습니다

### 성능 고려사항

1. **주기적 조회**:
   - `/attendance-status`: 1~5분마다 조회 권장
   - `/work-time`: 1~2분마다 조회 권장
   - `/work-summary`: 필요할 때만 조회 (변화 적음)

2. **배치 조회**:
   - 가능하면 3개 엔드포인트를 순차적으로 조회
   - 병렬 조회 시 과부하 주의

3. **로컬 캐싱**:
   - 응답 결과를 로컬에 캐시하여 불필요한 조회 감소
   - 1~5분 유효기간 권장

### 오류 처리

1. **401 Unauthorized**:
   - AccessToken 누락 또는 만료
   - RefreshToken으로 재발급 후 재시도

2. **404 Not Found**:
   - 사용자 정보가 없거나 삭제됨
   - 재로그인 필요

3. **5xx Server Error**:
   - 서버 측 오류
   - 재시도 로직 구현 권장

### 데이터 정확성

1. **실제 출퇴근 시간 vs 스케줄 시간**:
   - 근무 시간 계산은 실제 체크 시간을 기반으로 함
   - 스케줄 예정 시간과 다를 수 있음

2. **부분 근무 처리**:
   - 예정 시간 내에서만 계산됨
   - 예정 시간 외 체크는 제외됨

3. **다중 스케줄 인터페이스**:
   - 하루 여러 번 근무 시 모두 합산됨
   - 각 스케줄이 독립적으로 관리됨

---

## 📚 관련 문서

- [근무 일정 API](/docs/api/schedule.md): 근무 일정 신청/관리
- [출근 기록 API](/docs/api/attendance.md): 출퇴근 체크
- [인증 API](/docs/api/auth.md): 로그인/토큰 관리
- [에러 처리](/docs/conventions/error-handling.md): 에러 코드 및 처리 방법
- [데이터베이스 스키마](/docs/database/schema/): 테이블 구조
- [API 요약](/docs/api/endpoints-summary.md): 전체 엔드포인트 목록
