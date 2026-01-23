# 업무 관리 API (Task)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [업무 조회](#-업무-조회)
- [업무 생성](#-업무-생성)
- [업무 수정](#-업무-수정)
- [업무 완료](#-업무-완료)
- [업무 삭제](#-업무-삭제)
- [업무 일괄 저장](#-업무-일괄-저장)
- [업무 템플릿](#-업무-템플릿)
- [에러 처리](#-에러-처리)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

업무(Task) 관리 시스템은 정기/비정기 업무를 관리하고, 템플릿을 통해 반복 업무를 효율적으로 등록할 수 있는 기능을 제공합니다.

**주요 기능**:
- 일일 업무 조회 (정기/비정기로 구분)
- 업무 생성, 수정, 완료 상태 관리
- 업무 템플릿 생성 및 적용
- 관리자용 일괄 업무 관리

**Base Path**: `/api/v1/tasks` (사용자), `/api/v1/task-templates` (템플릿)

**업무 타입**:
| 코드 | 이름 | 설명 |
|------|------|------|
| `TT01` | 정기 업무 | 반복되는 업무 (예: 일일 회의, 정기 보고) |
| `TT02` | 비정기 업무 | 특정 날짜에만 진행하는 업무 |

---

## 🔐 인증

모든 엔드포인트는 **JWT AccessToken**이 필요합니다. `Authorization` 헤더로 전달됩니다.

**권한별 접근 제한**:
- 인증 필요: `/api/v1/tasks/**`, `/api/v1/task-templates/**`
- 관리자 전용(`@PreAuthorize("hasRole('RL02')")`):  
  - 업무 삭제, 업무 일괄 저장  
  - 템플릿 생성/수정/삭제/활성화/적용
- 그 외 엔드포인트는 인증만 필요하며 **소유자(본인) 검증은 현재 구현되지 않음**

---

## 📅 업무 조회

### 1.1 일별 업무 목록 조회

**Endpoint**: `GET /api/v1/tasks`

**설명**: 특정 날짜의 업무 목록을 정기/비정기, 오전/오후로 구분하여 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `date` | String | Yes | 조회할 날짜 (yyyy-MM-dd) |

**Example**:
```
GET /api/v1/tasks?date=2026-01-22
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무 목록을 조회했습니다.",
  "details": {
    "date": "2026-01-22",
    "regularTasks": {
      "morning": [
        {
          "taskId": 1,
          "title": "일일 회의",
          "assigneeId": 1,
          "assigneeName": "홍길동",
          "taskDate": "2026-01-22",
          "taskTime": "09:00:00",
          "taskType": "TT01",
          "taskTypeName": "정기 업무",
          "isCompleted": false
        },
        {
          "taskId": 2,
          "title": "업무 현황 보고",
          "assigneeId": 2,
          "assigneeName": "김철수",
          "taskDate": "2026-01-22",
          "taskTime": "10:00:00",
          "taskType": "TT01",
          "taskTypeName": "정기 업무",
          "isCompleted": true
        }
      ],
      "afternoon": [
        {
          "taskId": 3,
          "title": "마감 점검",
          "assigneeId": 1,
          "assigneeName": "홍길동",
          "taskDate": "2026-01-22",
          "taskTime": "15:00:00",
          "taskType": "TT01",
          "taskTypeName": "정기 업무",
          "isCompleted": false
        }
      ]
    },
    "irregularTasks": [
      {
        "taskId": 4,
        "title": "클라이언트 미팅",
        "assigneeId": 3,
        "assigneeName": "이영희",
        "taskDate": "2026-01-22",
        "taskTime": "14:00:00",
        "taskType": "TT02",
        "taskTypeName": "비정기 업무",
        "isCompleted": false
      }
    ]
  }
}
```

**분류 기준**:
- **정기 업무**: `taskType = TT01`
  - 오전 (09:00~12:00): `morning`
  - 오후 (12:00~18:00): `afternoon`
- **비정기 업무**: `taskType = TT02` (시간별로 정렬)

---

### 1.2 업무 단건 조회

**Endpoint**: `GET /api/v1/tasks/{taskId}`

**설명**: 특정 업무의 상세 정보를 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `taskId` | Long | 조회할 업무 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무를 조회했습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의",
    "assigneeId": 1,
    "assigneeName": "홍길동",
    "taskDate": "2026-01-22",
    "taskTime": "09:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": false
  }
}
```

**Error (404 Not Found)**:
```json
{
  "isSuccess": false,
  "message": "업무를 찾을 수 없습니다.",
  "details": null
}
```

---

## ✨ 업무 생성

### 2.1 업무 단건 생성

**Endpoint**: `POST /api/v1/tasks`

**설명**: 새로운 업무를 생성합니다. (인증 필요)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "title": "일일 회의",
  "assigneeId": 1,
  "taskDate": "2026-01-22",
  "taskTime": "09:00:00",
  "taskType": "TT01"
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | Yes | 업무명 (최대 200자) |
| `assigneeId` | Integer | Yes | 담당자 ID |
| `taskDate` | LocalDate | Yes | 업무 날짜 (yyyy-MM-dd) |
| `taskTime` | LocalTime | Yes | 업무 시간 (HH:mm:ss) |
| `taskType` | String | No | 업무 타입 (TT01/TT02), 기본값: TT01 |

#### Response

**Success (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "업무가 생성되었습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의",
    "assigneeId": 1,
    "assigneeName": "홍길동",
    "taskDate": "2026-01-22",
    "taskTime": "09:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": false
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `VALIDATION_ERROR` | 필수 입력값 누락 | title, assigneeId, taskDate, taskTime 누락 |
| 404 | `USER_NOT_FOUND` | 담당자를 찾을 수 없습니다. | 지정된 assigneeId가 존재하지 않음 |

---

## ✏️ 업무 수정

### 3.1 업무 정보 수정

**Endpoint**: `PATCH /api/v1/tasks/{taskId}`

**설명**: 기존 업무의 정보를 수정합니다.  
※ 현재 구현상 **본인 여부 검증이 없습니다** (인증된 사용자라면 수정 가능).

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `taskId` | Long | 수정할 업무 ID |

**Body** (선택적 필드):
```json
{
  "title": "일일 회의 (수정됨)",
  "assigneeId": 2,
  "taskTime": "10:00:00"
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | No | 업무명 (최대 200자) |
| `assigneeId` | Integer | No | 담당자 ID |
| `taskTime` | LocalTime | No | 업무 시간 (HH:mm:ss) |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무가 수정되었습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의 (수정됨)",
    "assigneeId": 2,
    "assigneeName": "김철수",
    "taskDate": "2026-01-22",
    "taskTime": "10:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": false
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 404 | `TASK_NOT_FOUND` | 업무를 찾을 수 없습니다. | taskId가 존재하지 않음 |
| 404 | `USER_NOT_FOUND` | 담당자를 찾을 수 없습니다. | assigneeId가 존재하지 않음 |

---

## ✅ 업무 완료

### 4.1 업무 완료 상태 토글

**Endpoint**: `PATCH /api/v1/tasks/{taskId}/toggle-complete`

**설명**: 업무의 완료 상태를 토글합니다 (완료 ↔ 미완료).  
※ 현재 구현상 **본인 여부 검증이 없습니다** (인증된 사용자라면 변경 가능).

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `taskId` | Long | 토글할 업무 ID |

#### Response

**Success - 완료 처리 (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무를 완료 처리했습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의",
    "assigneeId": 1,
    "assigneeName": "홍길동",
    "taskDate": "2026-01-22",
    "taskTime": "09:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": true
  }
}
```

**Success - 미완료 처리 (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무를 미완료 처리했습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의",
    "assigneeId": 1,
    "assigneeName": "홍길동",
    "taskDate": "2026-01-22",
    "taskTime": "09:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": false
  }
}
```

---

### 4.2 업무 완료 상태 설정

**Endpoint**: `PATCH /api/v1/tasks/{taskId}/complete`

**설명**: 업무의 완료 상태를 특정 값으로 설정합니다 (완료 또는 미완료).  
※ 현재 구현상 **본인 여부 검증이 없습니다** (인증된 사용자라면 변경 가능).

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `taskId` | Long | 설정할 업무 ID |

**Body**:
```json
{
  "isCompleted": true
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `isCompleted` | Boolean | Yes | 완료 여부 (true: 완료, false: 미완료) |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무를 완료 처리했습니다.",
  "details": {
    "taskId": 1,
    "title": "일일 회의",
    "assigneeId": 1,
    "assigneeName": "홍길동",
    "taskDate": "2026-01-22",
    "taskTime": "09:00:00",
    "taskType": "TT01",
    "taskTypeName": "정기 업무",
    "isCompleted": true
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 404 | `TASK_NOT_FOUND` | 업무를 찾을 수 없습니다. | taskId가 존재하지 않음 |

---

## 🗑️ 업무 삭제

### 5.1 업무 삭제

**Endpoint**: `DELETE /api/v1/tasks/{taskId}`

**설명**: 업무를 삭제합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `taskId` | Long | 삭제할 업무 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무가 삭제되었습니다.",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `TASK_NOT_FOUND` | 업무를 찾을 수 없습니다. | taskId가 존재하지 않음 |

---

## 📦 업무 일괄 저장

### 6.1 업무 일괄 생성/수정

**Endpoint**: `PUT /api/v1/tasks/batch`

**설명**: 여러 업무를 한 번에 생성하거나 수정합니다. (관리자 전용)
- `taskId`가 있으면 수정
- `taskId`가 없으면 생성
- 일부 실패해도 성공한 항목은 저장됩니다 (부분 성공)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "date": "2026-01-22",
  "tasks": [
    {
      "taskId": 1,
      "title": "일일 회의 (수정)",
      "assigneeId": 1,
      "taskTime": "09:00:00",
      "taskType": "TT01",
      "isCompleted": false
    },
    {
      "title": "새로운 업무",
      "assigneeId": 2,
      "taskTime": "14:00:00",
      "taskType": "TT02"
    }
  ]
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `date` | LocalDate | Yes | 업무 날짜 (yyyy-MM-dd) |
| `tasks` | Array | Yes | 업무 항목 배열 |
| `tasks[].taskId` | Long | No | 업무 ID (없으면 생성, 있으면 수정) |
| `tasks[].title` | String | Yes | 업무명 |
| `tasks[].assigneeId` | Integer | Yes | 담당자 ID |
| `tasks[].taskTime` | LocalTime | Yes | 업무 시간 |
| `tasks[].taskType` | String | No | 업무 타입 (기본: TT01) |
| `tasks[].isCompleted` | Boolean | No | 완료 여부 |

#### Response

**Success - 전체 성공 (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "업무가 저장되었습니다. (생성: 1개, 수정: 1개)",
  "details": {
    "totalCreated": 1,
    "totalUpdated": 1,
    "totalErrors": 0,
    "createdTasks": [
      {
        "taskId": 2,
        "title": "새로운 업무",
        "assigneeId": 2,
        "assigneeName": "김철수",
        "taskDate": "2026-01-22",
        "taskTime": "14:00:00",
        "taskType": "TT02",
        "taskTypeName": "비정기 업무",
        "isCompleted": false
      }
    ],
    "updatedTasks": [
      {
        "taskId": 1,
        "title": "일일 회의 (수정)",
        "assigneeId": 1,
        "assigneeName": "홍길동",
        "taskDate": "2026-01-22",
        "taskTime": "09:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "isCompleted": false
      }
    ],
    "errors": []
  }
}
```

**Partial Success - 부분 성공 (207 Multi-Status)**:
```json
{
  "isSuccess": false,
  "message": "일부 업무 저장에 실패했습니다. (성공: 1개, 실패: 1개)",
  "details": {
    "totalCreated": 1,
    "totalUpdated": 0,
    "totalErrors": 1,
    "createdTasks": [
      {
        "taskId": 2,
        "title": "새로운 업무",
        "assigneeId": 2,
        "assigneeName": "김철수",
        "taskDate": "2026-01-22",
        "taskTime": "14:00:00",
        "taskType": "TT02",
        "taskTypeName": "비정기 업무",
        "isCompleted": false
      }
    ],
    "updatedTasks": [],
    "errors": [
      {
        "taskId": 1,
        "title": "일일 회의",
        "errorMessage": "담당자를 찾을 수 없습니다."
      }
    ]
  }
}
```

**Error - 전체 실패 (422 Unprocessable Entity)**:
```json
{
  "isSuccess": false,
  "message": "모든 업무 저장에 실패했습니다.",
  "details": {
    "totalCreated": 0,
    "totalUpdated": 0,
    "totalErrors": 2,
    "createdTasks": [],
    "updatedTasks": [],
    "errors": [
      {
        "taskId": null,
        "title": "업무1",
        "errorMessage": "담당자를 찾을 수 없습니다."
      },
      {
        "taskId": null,
        "title": "업무2",
        "errorMessage": "담당자를 찾을 수 없습니다."
      }
    ]
  }
}
```

---

## 📋 업무 템플릿

템플릿을 사용하여 반복 업무를 효율적으로 관리할 수 있습니다.

### 7.1 템플릿 목록 조회

**Endpoint**: `GET /api/v1/task-templates`

**설명**: 등록된 업무 템플릿 목록을 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `activeOnly` | Boolean | No | 활성화된 템플릿만 조회 (기본: false) |

**Example**:
```
GET /api/v1/task-templates?activeOnly=true
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "템플릿 목록을 조회했습니다.",
  "details": {
    "templates": [
      {
        "templateId": 1,
        "templateName": "평일 기본 업무",
        "description": "월~금 정기 업무 템플릿",
        "isActive": true,
        "itemCount": 3
      },
      {
        "templateId": 2,
        "templateName": "주간 회의",
        "description": "월요일 정기 회의",
        "isActive": true,
        "itemCount": 2
      }
    ]
  }
}
```

---

### 7.2 템플릿 상세 조회

**Endpoint**: `GET /api/v1/task-templates/{templateId}`

**설명**: 특정 템플릿의 상세 정보와 항목들을 조회합니다.

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `templateId` | Long | 조회할 템플릿 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "템플릿을 조회했습니다.",
  "details": {
    "templateId": 1,
    "templateName": "평일 기본 업무",
    "description": "월~금 정기 업무 템플릿",
    "isActive": true,
    "items": [
      {
        "itemId": 1,
        "title": "일일 회의",
        "defaultAssigneeId": 1,
        "defaultAssigneeName": "홍길동",
        "taskTime": "09:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 1
      },
      {
        "itemId": 2,
        "title": "업무 현황 보고",
        "defaultAssigneeId": 2,
        "defaultAssigneeName": "김철수",
        "taskTime": "15:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 2
      }
    ]
  }
}
```

**Error (404 Not Found)**:
```json
{
  "isSuccess": false,
  "message": "템플릿을 찾을 수 없습니다.",
  "details": null
}
```

---

### 7.3 템플릿 생성

**Endpoint**: `POST /api/v1/task-templates`

**설명**: 새로운 업무 템플릿을 생성합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body**:
```json
{
  "templateName": "평일 기본 업무",
  "description": "월~금 정기 업무 템플릿",
  "items": [
    {
      "title": "일일 회의",
      "defaultAssigneeId": 1,
      "taskTime": "09:00:00",
      "taskType": "TT01",
      "displayOrder": 1
    },
    {
      "title": "업무 현황 보고",
      "defaultAssigneeId": 2,
      "taskTime": "15:00:00",
      "taskType": "TT01",
      "displayOrder": 2
    }
  ]
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `templateName` | String | Yes | 템플릿 이름 (최대 100자, 중복 불가) |
| `description` | String | No | 템플릿 설명 (최대 500자) |
| `items` | Array | No | 템플릿 항목 배열 |
| `items[].title` | String | Yes | 업무명 |
| `items[].defaultAssigneeId` | Integer | No | 기본 담당자 ID |
| `items[].taskTime` | LocalTime | Yes | 업무 시간 (HH:mm:ss) |
| `items[].taskType` | String | No | 업무 타입 (기본: TT01) |
| `items[].displayOrder` | Integer | No | 표시 순서 |

#### Response

**Success (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "템플릿이 생성되었습니다.",
  "details": {
    "templateId": 1,
    "templateName": "평일 기본 업무",
    "description": "월~금 정기 업무 템플릿",
    "isActive": true,
    "items": [
      {
        "itemId": 1,
        "title": "일일 회의",
        "defaultAssigneeId": 1,
        "defaultAssigneeName": "홍길동",
        "taskTime": "09:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 1
      },
      {
        "itemId": 2,
        "title": "업무 현황 보고",
        "defaultAssigneeId": 2,
        "defaultAssigneeName": "김철수",
        "taskTime": "15:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 2
      }
    ]
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `VALIDATION_ERROR` | 필수 입력값 누락 | templateName 누락 |
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `USER_NOT_FOUND` | 담당자를 찾을 수 없습니다. | items의 defaultAssigneeId가 존재하지 않음 |
| 409 | `DUPLICATE_TEMPLATE_NAME` | 같은 이름의 템플릿이 이미 존재합니다. | templateName 중복 |

---

### 7.4 템플릿 수정

**Endpoint**: `PUT /api/v1/task-templates/{templateId}`

**설명**: 기존 템플릿의 정보를 수정합니다. items가 제공되면 기존 항목을 교체합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `templateId` | Long | 수정할 템플릿 ID |

**Body** (선택적 필드):
```json
{
  "templateName": "평일 기본 업무 (수정)",
  "description": "월~금 정기 업무 템플릿 (더 상세함)",
  "items": [
    {
      "title": "아침 회의",
      "defaultAssigneeId": 1,
      "taskTime": "08:30:00",
      "taskType": "TT01",
      "displayOrder": 1
    },
    {
      "title": "오후 보고",
      "defaultAssigneeId": 2,
      "taskTime": "16:00:00",
      "taskType": "TT01",
      "displayOrder": 2
    }
  ]
}
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "템플릿이 수정되었습니다.",
  "details": {
    "templateId": 1,
    "templateName": "평일 기본 업무 (수정)",
    "description": "월~금 정기 업무 템플릿 (더 상세함)",
    "isActive": true,
    "items": [
      {
        "itemId": 1,
        "title": "아침 회의",
        "defaultAssigneeId": 1,
        "defaultAssigneeName": "홍길동",
        "taskTime": "08:30:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 1
      },
      {
        "itemId": 2,
        "title": "오후 보고",
        "defaultAssigneeId": 2,
        "defaultAssigneeName": "김철수",
        "taskTime": "16:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "displayOrder": 2
      }
    ]
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `TEMPLATE_NOT_FOUND` | 템플릿을 찾을 수 없습니다. | templateId가 존재하지 않음 |
| 404 | `USER_NOT_FOUND` | 담당자를 찾을 수 없습니다. | items의 defaultAssigneeId가 존재하지 않음 |
| 409 | `DUPLICATE_TEMPLATE_NAME` | 같은 이름의 템플릿이 이미 존재합니다. | templateName 중복 |

---

### 7.5 템플릿 삭제

**Endpoint**: `DELETE /api/v1/task-templates/{templateId}`

**설명**: 템플릿을 삭제합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `templateId` | Long | 삭제할 템플릿 ID |

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "템플릿이 삭제되었습니다.",
  "details": null
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `TEMPLATE_NOT_FOUND` | 템플릿을 찾을 수 없습니다. | templateId가 존재하지 않음 |

---

### 7.6 템플릿 활성화/비활성화

**Endpoint**: `PATCH /api/v1/task-templates/{templateId}/active`

**설명**: 템플릿의 활성화 상태를 변경합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `templateId` | Long | 변경할 템플릿 ID |

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `isActive` | Boolean | Yes | 활성화 여부 |

**Example**:
```
PATCH /api/v1/task-templates/1/active?isActive=false
```

#### Response

**Success (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "템플릿이 비활성화되었습니다.",
  "details": {
    "templateId": 1,
    "templateName": "평일 기본 업무",
    "isActive": false,
    "itemCount": 2
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `TEMPLATE_NOT_FOUND` | 템플릿을 찾을 수 없습니다. | templateId가 존재하지 않음 |

---

### 7.7 템플릿 적용

**Endpoint**: `POST /api/v1/task-templates/{templateId}/apply`

**설명**: 템플릿을 특정 날짜에 적용하여 업무를 일괄 생성합니다. (관리자 전용)

#### Request

**Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `templateId` | Long | 적용할 템플릿 ID |

**Body**:
```json
{
  "targetDate": "2026-01-22",
  "assigneeOverrides": [
    {
      "itemId": 1,
      "assigneeId": 3
    }
  ]
}
```

**필드 설명**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `targetDate` | LocalDate | Yes | 적용할 날짜 (yyyy-MM-dd) |
| `assigneeOverrides` | Array | No | 담당자 오버라이드 (선택사항) |
| `assigneeOverrides[].itemId` | Long | Yes | 템플릿 항목 ID |
| `assigneeOverrides[].assigneeId` | Integer | Yes | 새로운 담당자 ID |

**사용 사례**:
```
평일 기본 업무 템플릿을 2026-01-22에 적용하되,
템플릿에서는 홍길동이 일일 회의를 담당하지만,
이 날만 특별히 김철수가 담당하도록 변경 가능
```

#### Response

**Success (201 Created)**:
```json
{
  "isSuccess": true,
  "message": "템플릿이 적용되어 2개의 업무가 생성되었습니다.",
  "details": {
    "templateId": 1,
    "templateName": "평일 기본 업무",
    "targetDate": "2026-01-22",
    "createdCount": 2,
    "tasks": [
      {
        "taskId": 10,
        "title": "일일 회의",
        "assigneeId": 3,
        "assigneeName": "이영희",
        "taskDate": "2026-01-22",
        "taskTime": "09:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "isCompleted": false
      },
      {
        "taskId": 11,
        "title": "업무 현황 보고",
        "assigneeId": 2,
        "assigneeName": "김철수",
        "taskDate": "2026-01-22",
        "taskTime": "15:00:00",
        "taskType": "TT01",
        "taskTypeName": "정기 업무",
        "isCompleted": false
      }
    ]
  }
}
```

**Error Responses**:

| Status | Error Code | Message | 설명 |
|--------|-----------|---------|------|
| 400 | `EMPTY_TEMPLATE_ITEMS` | 템플릿에 항목이 없습니다. | 빈 템플릿 적용 시도 |
| 403 | `FORBIDDEN` | 권한 없음 | 관리자 권한 없음 |
| 404 | `TEMPLATE_NOT_FOUND` | 템플릿을 찾을 수 없습니다. | templateId가 존재하지 않음 |
| 404 | `USER_NOT_FOUND` | 담당자를 찾을 수 없습니다. | assigneeOverrides의 assigneeId가 존재하지 않음 |

---

## 🔐 에러 처리

### 공통 에러 코드

| HTTP Status | Error Code | 설명 |
|------------|-----------|------|
| 400 | `VALIDATION_ERROR` | 입력값 검증 실패 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `TASK_NOT_FOUND` | 업무를 찾을 수 없음 |
| 404 | `TEMPLATE_NOT_FOUND` | 템플릿을 찾을 수 없음 |
| 404 | `USER_NOT_FOUND` | 사용자를 찾을 수 없음 |
| 409 | `DUPLICATE_TEMPLATE_NAME` | 템플릿 이름 중복 |

### 에러 응답 형식

```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "errorReason": "상세 이유",
    "receivedValue": "입력받은 값"
  }
}
```

---

## 📝 사용 예시

### 예시 1: 일일 업무 조회 및 완료 처리

```bash
# 1단계: 오늘 업무 목록 조회
curl -X GET "http://localhost:8080/api/v1/tasks?date=2026-01-22" \

# 2단계: 특정 업무의 완료 상태 토글
curl -X PATCH "http://localhost:8080/api/v1/tasks/1/toggle-complete" \
```

### 예시 2: 템플릿 생성 및 적용

```bash
# 1단계: 평일 기본 업무 템플릿 생성 (관리자)
curl -X POST "http://localhost:8080/api/v1/task-templates" \
  -H "Content-Type: application/json" \
  -d '{
    "templateName": "평일 기본 업무",
    "description": "월~금 정기 업무",
    "items": [
      {
        "title": "일일 회의",
        "defaultAssigneeId": 1,
        "taskTime": "09:00:00",
        "taskType": "TT01",
        "displayOrder": 1
      },
      {
        "title": "업무 보고",
        "defaultAssigneeId": 2,
        "taskTime": "15:00:00",
        "taskType": "TT01",
        "displayOrder": 2
      }
    ]
  }'

# 2단계: 내일(1월 23일)에 템플릿 적용
curl -X POST "http://localhost:8080/api/v1/task-templates/1/apply" \
  -H "Content-Type: application/json" \
  -d '{
    "targetDate": "2026-01-23",
    "assigneeOverrides": []
  }'

# 3단계: 내일 업무 목록 확인
curl -X GET "http://localhost:8080/api/v1/tasks?date=2026-01-23" \
```

### 예시 3: 일괄 업무 저장

```bash
# 관리자가 여러 업무를 한 번에 생성/수정
curl -X PUT "http://localhost:8080/api/v1/tasks/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-01-22",
    "tasks": [
      {
        "title": "긴급 회의",
        "assigneeId": 1,
        "taskTime": "11:00:00",
        "taskType": "TT02"
      },
      {
        "taskId": 5,
        "title": "점심 미팅 (변경)",
        "assigneeId": 3,
        "taskTime": "12:30:00"
      }
    ]
  }'
```

---

## 🔗 관련 문서

### 연관 API
- [근무 일정 API](./schedule.md) - 근무 시간 관리
- [대시보드 API](./home.md) - 홈 화면 요약
- [관리자 API](./admin.md) - 관리자 기능

### 규약 및 시스템
- [에러 처리 규약](../conventions/error-handling.md)
- [API 설계 규약](../conventions/api-conventions.md)
- [코드 시스템](../database/schema/code-system.md) - TaskType(`TT`)

### 데이터베이스
- [Task 스키마](../database/README.md)
- [TaskTemplate 스키마](../database/README.md)

### 상위 문서
- ⬆️ [API 문서 홈](./README.md)
- ⬆️ [문서 허브](../README.md)
