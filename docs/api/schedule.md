# 근무 일정 API (Work Schedule)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [근무 일정 신청](#-근무-일정-신청)
- [근무 일정 수정](#-근무-일정-수정)
- [근무 일정 조회](#-근무-일정-조회)
- [근무 일정 취소](#-근무-일정-취소)
- [검증 규칙](#-검증-규칙)
- [관련 문서](#-관련-문서)

---

## 📖 개요

사용자의 근무 일정을 관리하는 API입니다. 일정 신청, 수정, 조회, 취소 기능을 제공하며,
복잡한 비즈니스 검증 로직(시간 제한, 동시 인원 제한 등)을 포함합니다.

**Base Path**: `/api/work-schedules`

**주요 기능**:
- 근무 일정 일괄 신청 (부분 성공/실패 지원)
- 기존 일정 수정 (시간 보존 원칙)
- 월별/이력 조회
- 일정 취소 (즉시/요청)

---

## 🔐 인증

**인증 방식**: `Authorization: Bearer <AccessToken>` 헤더로 JWT AccessToken 전달

**인증 처리**:
- Spring Security에서 강제 인증하지 않음 (SecurityConfig에서 `permitAll`)
- 컨트롤러에서 `@AuthenticationPrincipal CustomUserDetails userDetails` 파라미터로 인증 정보 요청
- **실제 호출 시에는 반드시 AccessToken을 포함**해야 함

**범위 제한**:
- 본인의 일정만 조회/수정 가능
- 타인의 일정은 접근 불가능 (컨트롤러에서 userId 검증)
- 관리자 API(`/api/admin/*`)를 통해서만 전체 일정 조회/관리 가능

---

## 📝 근무 일정 신청

### 2.1 근무 일정 일괄 신청

**Endpoint**: `POST /api/work-schedules/apply`

**설명**: 여러 근무 시간대를 일괄 신청합니다. 신청 기간 내라면 자동 승인(`WS02`)되며, 기간 외라면 승인 대기 상태(`WS01`)로 등록됩니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
※ 컨트롤러가 `@AuthenticationPrincipal`을 사용하므로 실제 호출 시 토큰이 필요합니다.  
※ 현재 구현은 사용자 ID를 인증 정보가 아닌 **고정값(1)**으로 사용합니다.

**Body**:
```json
{
  "slots": [
    {
      "start": "2026-01-22T09:00:00",
      "end": "2026-01-22T12:00:00"
    },
    {
      "start": "2026-01-22T14:00:00",
      "end": "2026-01-22T16:00:00"
    }
  ]
}
```

#### Response

**Success - 전체 성공 (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 등록되었습니다.",
  "details": {
    "success": [
      {
        "start": "2026-01-22T09:00:00",
        "end": "2026-01-22T12:00:00"
      },
      {
        "start": "2026-01-22T14:00:00",
        "end": "2026-01-22T16:00:00"
      }
    ],
    "fail": []
  }
}
```

**Partial Success - 부분 성공 (207 Multi-Status)**:
```json
{
  "isSuccess": false,
  "message": "신청하신 일정 중 실패한 일정이 존재합니다.",
  "details": {
    "success": [
      {
        "start": "2026-01-22T09:00:00",
        "end": "2026-01-22T12:00:00"
      }
    ],
    "fail": [
      {
        "start": "2026-01-22T14:00:00",
        "end": "2026-01-22T16:00:00"
      }
    ]
  }
}
```

**Error - 전체 실패 (422 Unprocessable Entity)**:
```json
{
  "isSuccess": false,
  "message": "신청하신 일정이 모두 실패하였습니다.",
  "details": {
    "success": [],
    "failure": [
      {
        "start": "2026-01-22T09:00:00",
        "end": "2026-01-22T10:30:00"
      }
    ]
  }
}
```

#### 상태 결정 로직

| 신청 시점 | 상태 | 코드 | 설명 |
|----------|------|------|------|
| **신청 기간 내** | 승인됨 | `WS02` | 즉시 승인, 검증 통과 시 바로 근무 가능 |
| **신청 기간 외** | 신청됨 | `WS01` | 관리자 승인 대기 |

#### 검증 규칙

모든 신청은 다음 검증을 통과해야 합니다:

1. **최소 근무 시간**: 1회 **2시간 이상** (`MIN_WORK_TIME_NOT_MET`)
2. **월 총량 제한**: 월 최대 **27시간** (`TOTAL_WORK_TIME_EXCEEDED`)
3. **주 총량 제한**: 주 최대 **13시간** (`WEEKLY_WORK_TIME_EXCEEDED`)
4. **동시 근무 인원**: 15분 단위로 겹치는 인원이 `MonthlyScheduleConfig.maxConcurrent`를 초과하는지 검사

#### Edge Cases

| 상황 | 처리 방식 |
|------|----------|
| **부분 성공** | 3개 신청 중 1개만 시간 겹침으로 실패 → 2개는 저장, 1개는 실패 응답에 포함 (207) |
| **동시성 이슈** | 마지막 1자리를 두고 동시에 신청 시 DB 락 또는 애플리케이션 레벨 검증으로 처리 |
| **과거 날짜 신청** | 현재 로직에 과거 날짜 검증이 없음 |

---

## ✏️ 근무 일정 수정

### 3.1 근무 일정 수정

**Endpoint**: `PATCH /api/work-schedules/modify`

**설명**: 기존 일정을 취소하고 새로운 일정을 추가합니다. **"근무 시간 보존의 법칙"** - 취소하는 총 시간과 추가하는 총 시간이 동일해야 합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "applySlots": [
    {
      "start": "2026-01-25T10:00:00",
      "end": "2026-01-25T13:00:00"
    }
  ],
  "cancelScheduleIds": [123],
  "reason": "일정 변경 요청"
}
```

#### Response

**Success (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 수정(요청)되었습니다.",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `WORK_DURATION_MISMATCH` | 삭제하는 일정의 총 근무 시간과 추가하는 일정의 총 근무 시간이 일치하지 않습니다. | 시간 보존 원칙 위반 (details에 실패 슬롯이 포함될 수 있음) |
| 400 | `PAST_MONTH_MODIFICATION_NOT_ALLOWED` | 지난 달의 근무 일정은 수정할 수 없습니다. | 현재 월 이전 일정 수정 시도 |
| 404 | `SCHEDULE_NOT_FOUND` | 해당 근무 일정을 찾을 수 없습니다. | 취소할 일정 ID가 존재하지 않음 |
| 403 | `UNAUTHORIZED_ACCESS` | 해당 근무 일정에 대한 권한이 없습니다. | 타인의 일정 수정 시도 |

#### 수정 로직

**취소 로직**:
- **신청 기간 내**: 즉시 취소 (`WS04`)
- **신청 기간 외**: 취소 요청 (`CR02` + `CS01`)

**추가 로직**:
- **신청 기간 내**: 즉시 승인 (`WS02`)
- **신청 기간 외**: 변경(추가) 요청 (`CR01` + `CS01`)
- 추가 시 모든 Validator(시간 총량, 동시 인원) 재검증

#### Edge Cases

| 상황 | 처리 방식 |
|------|----------|
| **시간 불일치** | 취소는 2시간인데 추가를 3시간 하려고 하면 전체 롤백 |
| **삭제 대상이 변경 요청 중** | 상태 충돌 가능성 있음 |
| **지난 달 일정** | 수정 불가 |

---

## 📅 근무 일정 조회

### 4.1 월별 일정 조회

**Endpoint**: `GET /api/work-schedules`

**설명**: 특정 월의 나의 근무 일정 목록을 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `year` | Integer | Yes | 조회할 년도 (예: 2026) |
| `month` | Integer | Yes | 조회할 월 (1-12) |

**Example**:
```
GET /api/work-schedules?year=2026&month=1
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 일정 조회 성공",
  "details": {
    "schedules": [
      {
        "id": 123,
        "start": "2026-01-22T09:00:00",
        "end": "2026-01-22T12:00:00",
        "status": "WS02"
      },
      {
        "id": 124,
        "start": "2026-01-25T14:00:00",
        "end": "2026-01-25T16:00:00",
        "status": "WS01"
      }
    ]
  }
}
```

---

### 4.2 근무 이력 조회

**Endpoint**: `GET /api/work-schedules/history`

**설명**: 특정 월의 근무 일정과 실제 출퇴근 기록을 함께 조회합니다. (실근무 시간 포함)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `year` | Integer | Yes | 조회할 년도 |
| `month` | Integer | Yes | 조회할 월 (1-12) |

**Example**:
```
GET /api/work-schedules/history?year=2026&month=1
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 이력 조회 성공",
  "details": {
    "histories": [
      {
        "id": 123,
        "start": "2026-01-22T09:00:00",
        "end": "2026-01-22T12:00:00",
        "status": "WS02",
        "actualStart": "2026-01-22T09:05:00",
        "actualEnd": "2026-01-22T12:00:00",
        "workDurationMinutes": 175
      },
      {
        "id": 124,
        "start": "2026-01-25T14:00:00",
        "end": "2026-01-25T17:00:00",
        "status": "WS02",
        "actualStart": "2026-01-25T14:00:00",
        "actualEnd": "2026-01-25T17:00:00",
        "workDurationMinutes": 180
      }
    ]
  }
}
```

---

### 4.3 특정 일정 상세 조회

**Endpoint**: `GET /api/work-schedules/{scheduleId}`

**설명**: 특정 일정의 상세 정보를 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `scheduleId` | Integer | 조회할 일정 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 일정 상세 조회 성공",
  "details": {
    "id": 123,
    "start": "2026-01-22T09:00:00",
    "end": "2026-01-22T12:00:00",
    "status": "WS02"
  }
}
```

---

## 🗑️ 근무 일정 취소

### 5.1 일정 취소/삭제

**Endpoint**: `DELETE /api/work-schedules/{scheduleId}`

**설명**: 특정 일정을 취소합니다. 신청 기간 내라면 즉시 취소(`WS04`)되며, 기간 외라면 취소 요청(`CR02`)으로 등록됩니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `scheduleId` | Integer | 취소할 일정 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "근무 일정 취소(요청) 성공",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 404 | `SCHEDULE_NOT_FOUND` | 해당 근무 일정을 찾을 수 없습니다. | 존재하지 않는 일정 ID |
| 403 | `UNAUTHORIZED_ACCESS` | 해당 근무 일정에 대한 권한이 없습니다. | 타인의 일정 취소 시도 |
| 400 | `PAST_MONTH_MODIFICATION_NOT_ALLOWED` | 지난 달의 근무 일정은 수정할 수 없습니다. | 지난 달 일정 취소 시도 |

---

## ✅ 검증 규칙

### 시간 제한

| 제한 항목 | 기준 | 값 | 에러 코드 |
|----------|------|----|----|
| **최소 근무 시간** | 1회 | 2시간 (120분) | `MIN_WORK_TIME_NOT_MET` |
| **주간 최대 시간** | 7일 | 13시간 (780분) | `WEEKLY_WORK_TIME_EXCEEDED` |
| **월간 최대 시간** | 월 | 27시간 (1620분) | `TOTAL_WORK_TIME_EXCEEDED` |

### 동시 근무 인원 제한

- **검증 단위**: 15분 체크포인트(시작+15분부터 30분 간격)
- **기준**: `MonthlyScheduleConfig.maxConcurrent`
- **에러 코드**: `MAX_CONCURRENT_EXCEEDED`

**예시**:
```
09:00 - 09:15: 5명 (제한: 6명) ✅
09:15 - 09:30: 6명 (제한: 6명) ✅
09:30 - 09:45: 7명 (제한: 6명) ❌ 초과!
```

### CodeType 상태

| 코드 | 이름 | 설명 |
|------|------|------|
| `WS01` | 신청됨 | 관리자 승인 대기 |
| `WS02` | 승인됨 | 근무 가능 |
| `WS03` | 거부됨 | 관리자가 거부 |
| `WS04` | 취소됨 | 사용자 또는 관리자가 취소 |

---

## 🔧 실전 워크플로우 및 예제

### 시나리오 1: 신청 기간 내 일정 신청 (즉시 승인)

**상황**: 1월 23일 (신청 기간 내)에 사용자가 3개 일정 신청 → 2개 성공, 1개 동시인원 초과로 실패

**1️⃣ 신청 기간 확인**
```bash
curl -X GET "http://localhost:8080/api/admin/schedule/monthly-limit/2026/01" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json"

# 응답: applyStartTime: "2025-12-23", applyEndTime: "2025-12-27"
# 현재: 2026-01-23 → 신청 기간 외! (기간이 지났음)
```

**2️⃣ 일정 신청**
```bash
curl -X POST "http://localhost:8080/api/work-schedules/apply" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "slots": [
      {
        "start": "2026-01-25T09:00:00",
        "end": "2026-01-25T11:00:00"
      },
      {
        "start": "2026-01-25T14:00:00",
        "end": "2026-01-25T17:00:00"
      },
      {
        "start": "2026-01-25T19:00:00",
        "end": "2026-01-25T20:00:00"
      }
    ]
  }'
```

**3️⃣ 응답 (207 Multi-Status - 부분 성공)**
```json
{
  "isSuccess": false,
  "message": "신청하신 일정 중 실패한 일정이 존재합니다.",
  "details": {
    "success": [
      {"start": "2026-01-25T09:00:00", "end": "2026-01-25T11:00:00"},
      {"start": "2026-01-25T14:00:00", "end": "2026-01-25T17:00:00"}
    ],
    "fail": [
      {
        "start": "2026-01-25T19:00:00",
        "end": "2026-01-25T20:00:00",
        "reason": "동시 근무 인원 초과"
      }
    ]
  }
}
```

**4️⃣ 프론트엔드 처리**
```typescript
// TypeScript 예제
interface ApplyResponse {
  isSuccess: boolean;
  message: string;
  details: {
    success: Array<{start: string; end: string}>;
    fail: Array<{start: string; end: string; reason: string}>;
  };
}

async function applyWorkSchedule(slots: Slot[]) {
  const response = await fetch('/api/work-schedules/apply', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ slots: slots })
  });

  const data: ApplyResponse = await response.json();

  // 부분 성공 처리 (207)
  if (response.status === 207) {
    showAlert(`${data.details.success.length}개 승인, ${data.details.fail.length}개 실패`);
    data.details.fail.forEach(f => {
      console.log(`${f.start} ~ ${f.end}: ${f.reason}`);
    });
  }

  // 전체 실패 처리 (422)
  if (response.status === 422) {
    showError('모든 일정이 실패했습니다. 다시 시도하세요.');
  }

  return data;
}
```

---

### 시나리오 2: 검증 실패 시나리오

**❌ 에러 1: 최소 근무 시간 미충족 (2시간 미만)**
```bash
curl -X POST "http://localhost:8080/api/work-schedules/apply" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "slots": [
      {
        "start": "2026-01-25T09:00:00",
        "end": "2026-01-25T10:30:00"  # 1시간 30분만!
      }
    ]
  }'

# 응답 422
{
  "isSuccess": false,
  "message": "신청하신 일정이 모두 실패하였습니다.",
  "details": {
    "success": [],
    "fail": [
      {
        "start": "2026-01-25T09:00:00",
        "end": "2026-01-25T10:30:00",
        "reason": "최소 근무 시간(2시간) 미충족"
      }
    ]
  }
}
```

**❌ 에러 2: 월간 최대 시간 초과 (27시간)**
```bash
# 사용자가 이미 25시간 신청한 상태에서 4시간 추가 시도
curl -X POST "http://localhost:8080/api/work-schedules/apply" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "slots": [
      {
        "start": "2026-01-28T09:00:00",
        "end": "2026-01-28T13:00:00"  # 4시간 (총 29시간 = 초과!)
      }
    ]
  }'

# 응답 422
{
  "isSuccess": false,
  "message": "신청하신 일정이 모두 실패하였습니다.",
  "details": {
    "fail": [
      {
        "reason": "월간 최대 시간(27시간) 초과: 현재 25시간 + 신청 4시간 = 29시간"
      }
    ]
  }
}
```

**❌ 에러 3: 권한 없음 (타인의 일정 취소 시도)**
```bash
curl -X DELETE "http://localhost:8080/api/work-schedules/123" \
  -H "Authorization: Bearer <USER_A_TOKEN>"

# 응답 403 (123번 일정이 USER_B의 일정)
{
  "isSuccess": false,
  "message": "해당 근무 일정에 대한 권한이 없습니다.",
  "details": null
}
```

---

### 시나리오 3: 일정 수정 (시간 보존)

**상황**: 기존 2시간 일정을 삭제하고, 다른 시간에 2시간 추가

```bash
curl -X PATCH "http://localhost:8080/api/work-schedules/modify" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "cancelScheduleIds": [123],  # 2시간 일정
    "applySlots": [
      {
        "start": "2026-01-26T10:00:00",
        "end": "2026-01-26T12:00:00"  # 2시간 (시간 보존!)
      }
    ],
    "reason": "시간 변경 요청"
  }'

# 응답 201
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 수정(요청)되었습니다.",
  "details": null
}
```

**❌ 에러: 시간 불일치**
```bash
# 2시간 취소 but 3시간 추가 시도
{
  "cancelScheduleIds": [123],     # 2시간
  "applySlots": [
    {"start": "2026-01-26T10:00:00", "end": "2026-01-26T13:00:00"}  # 3시간
  ]
}

# 응답 400
{
  "isSuccess": false,
  "message": "취소 시간과 신청 시간이 일치하지 않습니다.",
  "details": {
    "cancelledTime": "120분",
    "appliedTime": "180분",
    "difference": "60분"
  }
}
```

---

## ⚠️ 자주 하는 실수 및 해결책

| 실수 | 원인 | 해결책 | 참고 |
|------|------|--------|------|
| **일정이 자동 승인 안 됨** | 신청 기간 외에 신청함 | 신청 기간 확인: `GET /admin/schedule/monthly-limit/{year}/{month}` | 기간 내만 즉시 승인 |
| **동시인원 초과 에러** | 동일 시간대에 인원 초과 | 다른 시간대로 변경 또는 기존 일정 삭제 후 재신청 | 15분 단위로 체크 |
| **최소 시간 미충족** | 2시간 미만 신청 | 최소 2시간 이상으로 신청 | 1회 최소 2시간 |
| **월간 제한 초과** | 월 27시간 초과 신청 | 다음 달로 연기 또는 기존 일정 취소 | 월 최대 27시간 |
| **수정 시 시간 불일치** | 취소/추가 시간 다름 | 취소와 추가 시간을 정확히 맞춤 | "시간 보존의 법칙" |
| **타인 일정 취소 불가** | 다른 사용자 일정 취소 시도 | 본인 일정만 취소 가능 | 관리자 API 사용 |
| **토큰 만료** | 오래된 AccessToken 사용 | `/auth/refresh`로 새 토큰 발급 | 1시간 유효 |

---

## 🔗 관련 문서

### 연관 API
- [관리자 API](./admin.md) - 월별 제한 설정, 변경 요청 처리
- [출퇴근 API](./attendance.md) - QR 체크인/아웃
- [대시보드 API](./home.md) - 근무 시간 요약

### 규약 및 시스템
- [에러 처리 규약](../conventions/error-handling.md)
- [API 설계 규약](../conventions/api-conventions.md)
- [코드 시스템](../database/schema/code-system.md) - WorkStatus(`WS`), ChangeRequest(`CR`)

### 데이터베이스
- [WorkSchedule 스키마](../database/README.md)
- [MonthlyScheduleLimit 스키마](../database/README.md)

### 상위 문서
- ⬆️ [API 문서 홈](./README.md)
- ⬆️ [문서 허브](../README.md)
