# 사용자 API (User)

## 📑 목차

- [개요](#-개요)
- [인증](#-인증)
- [주요 엔드포인트](#-주요-엔드포인트)
- [상세 엔드포인트 문서](#-상세-엔드포인트-문서)
- [에러 처리](#-에러-처리)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

사용자 마이페이지 정보 및 근무 시간 통계를 조회하는 API입니다.

사용자는 자신의 기본 정보, 주간 근무 시간, 월간 근무 시간을 조회할 수 있습니다.

**Base Path**: `/api/v1/users`

**태그**: `사용자 마이페이지`

---

## 🔐 인증

모든 엔드포인트는 **JWT Bearer Token 인증**이 필수입니다.

**Request Header**:
```
Authorization: Bearer {accessToken}
```

**예시**:
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🎯 주요 엔드포인트

| 메서드 | 경로 | 설명 | HTTP 상태 |
|--------|------|------|----------|
| GET | `/me` | 내 정보 조회 | 200 |
| GET | `/me/work-time/weekly` | 주간 근무 시간 조회 | 200 |
| GET | `/me/work-time/monthly` | 월간 근무 시간 조회 | 200 |

---

## 📋 상세 엔드포인트 문서

### 1️⃣ GET `/api/v1/users/me` - 내 정보 조회

**설명**: 현재 로그인한 사용자의 상세 정보를 조회합니다.

사용자의 기본 정보(ID, 이메일, 이름, 역할, 조직 등)를 반환합니다.

**Request**

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json"
```

**Response 200 OK** - 조회 성공

```json
{
  "isSuccess": true,
  "message": "내 정보 조회 성공",
  "details": {
    "userId": 1,
    "email": "student@example.com",
    "password": "(encrypted)",
    "userName": "김철수",
    "roleCode": "RL01",
    "organizationId": 1,
    "organizationName": "KU ICT",
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-24T14:30:00"
  }
}
```

**응답 필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 고유 ID |
| email | String | 로그인 이메일 주소 |
| userName | String | 사용자 이름 |
| roleCode | String | 사용자 역할 코드 (RL01: 학생, RL02: 관리자) |
| organizationId | Long | 소속 조직 ID |
| organizationName | String | 소속 조직 이름 |
| createdAt | DateTime | 계정 생성 일시 |
| updatedAt | DateTime | 계정 마지막 수정 일시 |

**에러 응답**

**401 Unauthorized** - 인증되지 않음

```json
{
  "isSuccess": false,
  "message": "인증이 필요합니다.",
  "details": null
}
```

**500 Internal Server Error** - 서버 오류

```json
{
  "isSuccess": false,
  "message": "사용자 정보 조회 중 오류가 발생했습니다.",
  "details": null
}
```

---

### 2️⃣ GET `/api/v1/users/me/work-time/weekly` - 주간 근무 시간 조회

**설명**: 현재 주(월요일 ~ 일요일)의 총 근무 시간을 조회합니다.

실제 출퇴근 기록을 기반으로 계산된 누적 근무 시간을 분(minutes) 단위로 반환합니다.

**Request**

```bash
curl -X GET http://localhost:8080/api/v1/users/me/work-time/weekly \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json"
```

**Response 200 OK** - 조회 성공

```json
{
  "isSuccess": true,
  "message": "주간 근무 시간 조회 성공",
  "details": {
    "userId": 1,
    "userName": "김철수",
    "totalMinutes": 240,
    "totalHours": 4,
    "period": "2025-01-20 (Mon) ~ 2025-01-26 (Sun)",
    "workDays": [
      {
        "date": "2025-01-20",
        "dayOfWeek": "Monday",
        "schedules": [
          {
            "startTime": "09:00",
            "endTime": "12:00",
            "status": "WS02",
            "workMinutes": 180
          }
        ],
        "dailyTotal": 180
      },
      {
        "date": "2025-01-21",
        "dayOfWeek": "Tuesday",
        "schedules": [
          {
            "startTime": "09:00",
            "endTime": "12:00",
            "status": "WS01",
            "workMinutes": 60
          }
        ],
        "dailyTotal": 60
      }
    ]
  }
}
```

**응답 필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| userName | String | 사용자 이름 |
| totalMinutes | Integer | 주간 총 근무 시간(분) |
| totalHours | Integer | 주간 총 근무 시간(시간) |
| period | String | 조회 기간 |
| workDays | Array | 요일별 근무 기록 배열 |
| workDays[].date | String | 날짜 (yyyy-MM-dd) |
| workDays[].dayOfWeek | String | 요일 (Monday ~ Sunday) |
| workDays[].schedules | Array | 해당 날짜의 일정 목록 |
| workDays[].dailyTotal | Integer | 해당 날짜 총 근무 시간(분) |

**로직**:

1. 현재 날짜 기준으로 그 주의 월요일 00:00 ~ 일요일 23:59 범위 설정
2. 해당 주의 모든 `WorkSchedule` 조회
3. 각 스케줄에 연결된 `WorkAttendance` 기록 확인
4. 시간 계산:
   - 출근(`CT01`) 기록 없음 → 0분
   - 퇴근(`CT02`) 기록 있음 → Duration(출근, 퇴근) 계산
   - 퇴근 기록 없음 → Duration(출근, 현재시간) 계산 (단, 스케줄 종료시간까지만)
   - 인정 시간은 스케줄 범위 내로 클램핑

**에러 응답**

**401 Unauthorized** - 인증되지 않음

```json
{
  "isSuccess": false,
  "message": "인증이 필요합니다.",
  "details": null
}
```

**Edge Cases**:
- **근무 일정 없음**: `totalMinutes: 0` 반환
- **지각**: 스케줄 시작보다 늦게 출근 시 실제 출근 시간부터 계산
- **조퇴/미퇴근**: 현재 시간이 스케줄 종료 전이면 실시간으로 근무 시간 증가

---

### 3️⃣ GET `/api/v1/users/me/work-time/monthly` - 월간 근무 시간 조회

**설명**: 현재 월(1일 ~ 말일)의 총 근무 시간을 조회합니다.

주간 조회와 동일한 로직으로, 월간 범위로 계산된 누적 근무 시간을 반환합니다.

**Request**

```bash
curl -X GET http://localhost:8080/api/v1/users/me/work-time/monthly \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json"
```

**Response 200 OK** - 조회 성공

```json
{
  "isSuccess": true,
  "message": "월간 근무 시간 조회 성공",
  "details": {
    "userId": 1,
    "userName": "김철수",
    "year": 2025,
    "month": 1,
    "totalMinutes": 2880,
    "totalHours": 48,
    "period": "2025-01 (January)",
    "workDays": [
      {
        "date": "2025-01-06",
        "dayOfWeek": "Monday",
        "schedules": [
          {
            "startTime": "09:00",
            "endTime": "17:00",
            "status": "WS02",
            "workMinutes": 480
          }
        ],
        "dailyTotal": 480
      },
      {
        "date": "2025-01-07",
        "dayOfWeek": "Tuesday",
        "schedules": [
          {
            "startTime": "09:00",
            "endTime": "17:00",
            "status": "WS02",
            "workMinutes": 480
          }
        ],
        "dailyTotal": 480
      },
      "... (더 많은 날짜들)"
    ]
  }
}
```

**응답 필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| userName | String | 사용자 이름 |
| year | Integer | 조회 연도 |
| month | Integer | 조회 월 |
| totalMinutes | Integer | 월간 총 근무 시간(분) |
| totalHours | Integer | 월간 총 근무 시간(시간) |
| period | String | 조회 기간 (yyyy-MM) |
| workDays | Array | 날짜별 근무 기록 배열 |

**로직**:

1. 현재 날짜 기준으로 그 월의 1일 00:00 ~ 말일 23:59 범위 설정
2. 해당 월의 모든 `WorkSchedule` 조회
3. 주간 조회와 동일한 시간 계산 로직 적용

---

## 🚨 에러 처리

### HTTP 상태 코드 매핑

| HTTP 상태 | 에러 | 설명 |
|----------|------|------|
| **200** | Success | 조회 성공 |
| **400** | Bad Request | 잘못된 요청 형식 |
| **401** | Unauthorized | 인증 토큰 없음 또는 만료됨 |
| **403** | Forbidden | 다른 사용자의 정보 접근 시도 |
| **500** | Internal Server Error | 서버 처리 중 오류 발생 |

### 에러 응답 형식

```json
{
  "isSuccess": false,
  "message": "오류 메시지",
  "details": null
}
```

### 공통 에러 시나리오

**1. 토큰 없음**

```bash
curl -X GET http://localhost:8080/api/v1/users/me
```

**응답 (401 Unauthorized)**:

```json
{
  "isSuccess": false,
  "message": "인증이 필요합니다.",
  "details": null
}
```

**해결 방법**: Authorization 헤더에 유효한 JWT 토큰 추가

---

**2. 토큰 만료**

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer expired_token_here"
```

**응답 (401 Unauthorized)**:

```json
{
  "isSuccess": false,
  "message": "토큰이 만료되었습니다.",
  "details": null
}
```

**해결 방법**: `/api/v1/auth/refresh` 엔드포인트로 새로운 AccessToken 발급

---

**3. 서버 오류**

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer {validToken}"
```

**응답 (500 Internal Server Error)**:

```json
{
  "isSuccess": false,
  "message": "사용자 정보 조회 중 오류가 발생했습니다.",
  "details": null
}
```

**해결 방법**: 서버 로그 확인 후 관리자에 문의

---

## 📚 사용 예시

### 예시 1: 사용자 정보와 주간 근무 시간을 함께 조회

**시나리오**: 사용자가 마이페이지 로드 시 자신의 정보와 주간 근무 시간을 표시

```bash
#!/bin/bash

ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Step 1: 내 정보 조회
echo "=== 내 정보 조회 ==="
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n=== 주간 근무 시간 조회 ==="
# Step 2: 주간 근무 시간 조회
curl -X GET http://localhost:8080/api/v1/users/me/work-time/weekly \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq '.'
```

**예상 응답**:

```json
{
  "isSuccess": true,
  "message": "내 정보 조회 성공",
  "details": {
    "userId": 1,
    "email": "student@example.com",
    "userName": "김철수",
    "roleCode": "RL01"
  }
}
{
  "isSuccess": true,
  "message": "주간 근무 시간 조회 성공",
  "details": {
    "totalMinutes": 240,
    "totalHours": 4
  }
}
```

---

### 예시 2: 월간 근무 시간으로 월간 통계 표시

**시나리오**: 사용자가 "이번 달 총 근무 시간" 통계를 확인

```bash
#!/bin/bash

ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8080/api/v1/users/me/work-time/monthly \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq '.details | {totalHours, period, workDays}'
```

**응답**:

```json
{
  "totalHours": 48,
  "period": "2025-01",
  "workDays": [
    {
      "date": "2025-01-06",
      "dailyTotal": 480
    },
    {
      "date": "2025-01-07",
      "dailyTotal": 480
    }
  ]
}
```

---

### 예시 3: 근무 일정 없는 경우 처리

**시나리오**: 사용자가 이번 주에 근무 일정이 없는 경우

```bash
#!/bin/bash

ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8080/api/v1/users/me/work-time/weekly \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" | jq '.'
```

**응답**:

```json
{
  "isSuccess": true,
  "message": "주간 근무 시간 조회 성공",
  "details": {
    "userId": 1,
    "userName": "김철수",
    "totalMinutes": 0,
    "totalHours": 0,
    "period": "2025-01-20 (Mon) ~ 2025-01-26 (Sun)",
    "workDays": []
  }
}
```

---

### 예시 4: JavaScript/TypeScript 클라이언트에서 호출

**시나리오**: 프론트엔드에서 사용자 정보 조회

```typescript
// user.service.ts

async function getUserInfo(accessToken: string): Promise<UserInfo> {
  const response = await fetch('http://localhost:8080/api/v1/users/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });

  const data = await response.json();

  if (!data.isSuccess) {
    throw new Error(data.message);
  }

  return data.details;
}

async function getWeeklyWorkTime(accessToken: string): Promise<WorkTimeResponse> {
  const response = await fetch('http://localhost:8080/api/v1/users/me/work-time/weekly', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });

  const data = await response.json();

  if (!data.isSuccess) {
    throw new Error(data.message);
  }

  return data.details;
}

// 사용 예
async function renderUserDashboard(accessToken: string) {
  const userInfo = await getUserInfo(accessToken);
  const weeklyTime = await getWeeklyWorkTime(accessToken);

  console.log(`안녕하세요, ${userInfo.userName}님!`);
  console.log(`이번 주 근무 시간: ${weeklyTime.totalHours}시간`);
}
```

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [인증 API](./auth.md)
- [근무 일정 API](./schedule.md)
- [출퇴근 API](./attendance.md)
- [홈 화면 API](./home.md)
- [데이터베이스 스키마 - User](../database/schema/user.md)
- [전체 엔드포인트 요약](./endpoints-summary.md)
