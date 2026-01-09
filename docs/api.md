# CommuteMate API Documentation

## Overview
CommuteMate 백엔드 시스템의 REST API 엔드포인트 문서입니다.

## Base URL
```
http://localhost:8080
```

## Authentication
대부분의 API는 JWT 인증이 필요합니다. 토큰은 다음과 같이 전달됩니다:
- **AccessToken**: HttpOnly 쿠키로 전달 (자동)
- **RefreshToken**: Authorization 헤더로 전달 (`Bearer <token>`)

---

## 1. Authentication API

### 1.1 Send Verification Code
이메일 인증 번호를 발송합니다.

**Endpoint**: `POST /api/v1/auth/send-verification-code`

**Request Body**:
```json
{
  "email": "user@example.com"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "인증번호가 이메일로 발송되었습니다. (유효시간: 5분)",
  "details": null
}
```

---

### 1.2 Verify Code
이메일 인증 번호를 검증합니다.

**Endpoint**: `POST /api/v1/auth/verify-code`

**Request Body**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.",
  "details": null
}
```

---

### 1.3 Register
회원가입을 진행합니다.

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "organizationId": 1,
  "roleCode": "RL01"
}
```

**Success Response**:
- **Status**: `201 Created`
```json
{
  "isSuccess": true,
  "message": "회원가입이 완료되었습니다.",
  "details": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

---

### 1.4 Login
로그인하여 액세스 토큰과 리프레시 토큰을 발급받습니다.

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Success Response**:
- **Status**: `200 OK`
- **Cookies**: `accessToken` (HttpOnly)
```json
{
  "isSuccess": true,
  "message": "로그인 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresAt": 1736485888000
  }
}
```

---

### 1.5 Logout
로그아웃합니다. 토큰을 블랙리스트에 등록합니다.

**Endpoint**: `POST /api/v1/auth/logout`

**Headers**: `Authorization: Bearer <accessToken>`

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "로그아웃되었습니다.",
  "details": null
}
```

---

### 1.6 Refresh Token
리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.

**Endpoint**: `POST /api/v1/auth/refresh`

**Headers**: `Authorization: Bearer <refreshToken>`

**Success Response**:
- **Status**: `200 OK`
- **Cookies**: `accessToken` (HttpOnly)
```json
{
  "isSuccess": true,
  "message": "토큰 갱신 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresAt": 1736485888000
  }
}
```

---

## 2. Work Schedule API (User)

### 2.1 Apply Work Schedule
근무 일정을 일괄 신청합니다.

**Endpoint**: `POST /api/v1/work-schedules/apply`

**Headers**: `userId: <userId>`

**Request Body**:
```json
{
  "slots": [
    {
      "start": "2025-12-01T09:00:00",
      "end": "2025-12-01T18:00:00"
    },
    {
      "start": "2025-12-02T09:00:00",
      "end": "2025-12-02T18:00:00"
    }
  ]
}
```

**Success Response**:
- **Status**: `201 Created`
```json
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 등록되었습니다.",
  "details": {
    "success": [
      {
        "scheduleId": 1,
        "startTime": "2025-12-01T09:00:00",
        "endTime": "2025-12-01T18:00:00"
      },
      {
        "scheduleId": 2,
        "startTime": "2025-12-02T09:00:00",
        "endTime": "2025-12-02T18:00:00"
      }
    ],
    "failure": []
  }
}
```

**Partial Failure Response**:
- **Status**: `207 Multi-Status`
```json
{
  "isSuccess": true,
  "message": "일부 일정 신청이 실패했습니다.",
  "details": {
    "success": [
      {
        "scheduleId": 1,
        "startTime": "2025-12-01T09:00:00",
        "endTime": "2025-12-01T18:00:00"
      }
    ],
    "failure": [
      {
        "reason": "이미 존재하는 일정입니다",
        "startTime": "2025-12-02T09:00:00",
        "endTime": "2025-12-02T18:00:00"
      }
    ]
  }
}
```

**All Failure Response**:
- **Status**: `422 Unprocessable Entity`
```json
{
  "isSuccess": false,
  "message": "모든 일정 신청이 실패했습니다.",
  "details": {
    "errorReason": "월별 제한을 초과했습니다",
    "failures": [
      {
        "reason": "월별 제한 초과",
        "startTime": "2025-12-01T09:00:00",
        "endTime": "2025-12-01T18:00:00"
      }
    ]
  }
}
```

---

### 2.2 Modify Work Schedule
기존 근무 일정을 취소하면서 새로운 일정을 추가 신청합니다.

**Endpoint**: `PATCH /api/v1/work-schedules/modify`

**Headers**: `userId: <userId>`

**Request Body**:
```json
{
  "cancelScheduleIds": [1, 2],
  "newSlots": [
    {
      "start": "2025-12-03T09:00:00",
      "end": "2025-12-03T18:00:00"
    }
  ]
}
```

**Success Response**:
- **Status**: `201 Created`
```json
{
  "isSuccess": true,
  "message": "신청하신 일정이 모두 수정(요청)되었습니다.",
  "details": null
}
```

---

## 3. Admin Schedule API

### 3.1 Set Monthly Limit
특정 연도/월의 최대 동시 근무 인원수를 설정합니다.

**Endpoint**: `POST /api/v1/admin/schedule/monthly-limit`

**Headers**: `userId: <userId>` (관리자 ID)

**Request Body**:
```json
{
  "scheduleYear": 2025,
  "scheduleMonth": 12,
  "maxConcurrent": 10
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한이 설정되었습니다.",
  "details": {
    "limitId": 1,
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "maxConcurrent": 10
  }
}
```

---

### 3.2 Get Monthly Limit
특정 연도/월의 스케줄 제한 설정을 조회합니다.

**Endpoint**: `GET /api/v1/admin/schedule/monthly-limit/{year}/{month}`

**Path Parameters**:
- `year` (integer): 연도 (예: 2025)
- `month` (integer): 월 (1-12)

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "월별 스케줄 제한을 조회했습니다.",
  "details": {
    "limitId": 1,
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "maxConcurrent": 10
  }
}
```

**Not Found Response**:
- **Status**: `404 Not Found`
```json
{
  "isSuccess": false,
  "message": "해당 월의 스케줄 제한 설정을 찾을 수 없습니다.",
  "details": null
}
```

---

### 3.3 Get All Monthly Limits
저장된 모든 월별 스케줄 제한 설정을 조회합니다.

**Endpoint**: `GET /api/v1/admin/schedule/monthly-limits`

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "모든 월별 스케줄 제한을 조회했습니다.",
  "details": {
    "limits": [
      {
        "limitId": 1,
        "scheduleYear": 2025,
        "scheduleMonth": 12,
        "maxConcurrent": 10
      },
      {
        "limitId": 2,
        "scheduleYear": 2025,
        "scheduleMonth": 11,
        "maxConcurrent": 8
      }
    ]
  }
}
```

---

### 3.4 Set Apply Term
특정 연도/월의 근무신청 가능 기간을 설정합니다.

**Endpoint**: `POST /api/v1/admin/schedule/set-apply-term`

**Headers**: `userId: <userId>` (관리자 ID)

**Request Body**:
```json
{
  "scheduleYear": 2025,
  "scheduleMonth": 12,
  "applyStartTime": "2025-11-23T00:00:00",
  "applyEndTime": "2025-11-27T23:59:59"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "신청 기간이 설정되었습니다.",
  "details": {
    "limitId": 1,
    "scheduleYear": 2025,
    "scheduleMonth": 12,
    "applyStartTime": "2025-11-23T00:00:00",
    "applyEndTime": "2025-11-27T23:59:59"
  }
}
```

**Invalid Request Response**:
- **Status**: `422 Unprocessable Entity`
```json
{
  "isSuccess": false,
  "message": "신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.",
  "details": {
    "errorReason": "신청 시작 시간이 종료 시간보다 늦거나 같습니다.",
    "receivedApplyStartTime": "2025-11-27T23:59:59",
    "receivedApplyEndTime": "2025-11-23T00:00:00"
  }
}
```

---

### 3.5 Process Change Request
근무 일정 변경 요청을 승인 또는 거부합니다.

**Endpoint**: `POST /api/v1/admin/schedule/process-change-request`

**Headers**: `userId: <userId>` (관리자 ID)

**Request Body**:
```json
{
  "requestIds": [1, 2],
  "statusCode": "CS02"
}
```

**Note**: `requestIds` 개수는 반드시 짝수여야 합니다. (쌍으로 처리하기 위함)
- `statusCode`: `CS02` (승인) 또는 `CS03` (거부)

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "변경 요청이 승인되었습니다.",
  "details": null
}
```

---

## 4. Category API

### 4.1 Register Category
새로운 대분류(Category)를 등록합니다.

**Endpoint**: `POST /api/v1/categories`

**Request Body**:
```json
{
  "name": "서비스이용"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "categoryId": 1,
  "name": "서비스이용"
}
```

**Conflict Response**:
- **Status**: `409 Conflict`
```json
{
  "isSuccess": false,
  "message": "이미 존재하는 대분류명입니다.",
  "details": null
}
```

---

### 4.2 Update Category
대분류 이름을 수정합니다.

**Endpoint**: `PUT /api/v1/categories/{categoryId}`

**Path Parameters**:
- `categoryId` (Long): 카테고리 ID

**Request Body**:
```json
{
  "name": "서비스 이용안내"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "categoryId": 1,
  "name": "서비스 이용안내"
}
```

---

### 4.3 Get All Categories
전체 대분류를 조회합니다.

**Endpoint**: `GET /api/v1/categories`

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "전체 카테고리 조회 성공",
  "details": {
    "categories": [
      {
        "categoryId": 1,
        "name": "서비스이용"
      },
      {
        "categoryId": 2,
        "name": "회원정보"
      }
    ]
  }
}
```

---

### 4.4 Delete Category
대분류를 삭제합니다.

**Endpoint**: `DELETE /api/v1/categories/{categoryId}`

**Path Parameters**:
- `categoryId` (Long): 카테고리 ID

**Success Response**:
- **Status**: `200 OK`
```json
{
  "isSuccess": true,
  "message": "성공적으로 삭제되었습니다.",
  "details": null
}
```

**Conflict Response**:
- **Status**: `409 Conflict`
```json
{
  "isSuccess": false,
  "message": "해당 카테고리에 속한 소분류가 있어 삭제할 수 없습니다.",
  "details": null
}
```

---

## 5. SubCategory API

### 5.1 Register SubCategory
새로운 소분류(SubCategory)를 등록합니다.

**Endpoint**: `POST /api/v1/subcategories`

**Request Body**:
```json
{
  "name": "회원가입",
  "categoryId": 1
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "subCategoryId": 1,
  "name": "회원가입",
  "categoryId": 1
}
```

---

### 5.2 Update SubCategory Name
소분류 이름을 수정합니다.

**Endpoint**: `PATCH /api/v1/subcategories/{subCategoryId}/name`

**Path Parameters**:
- `subCategoryId` (Long): 소분류 ID

**Request Body**:
```json
{
  "name": "회원가입 안내"
}
```

**Success Response**:
- **Status**: `200 OK`
```json
{
  "subCategoryId": 1,
  "name": "회원가입 안내"
}
```

---

### 5.3 Delete SubCategory
소분류를 삭제합니다.

**Endpoint**: `DELETE /api/v1/subcategories/{subCategoryId}`

**Path Parameters**:
- `subCategoryId` (Long): 소분류 ID

**Success Response**:
- **Status**: `200 OK`
```json
{
  "subCategoryId": 1,
  "deleted": true
}
```

**Conflict Response**:
- **Status**: `409 Conflict`
```json
{
  "isSuccess": false,
  "message": "해당 소분류에 속한 FAQ가 있어 삭제할 수 없습니다.",
  "details": null
}
```

---

### 5.4 Update SubCategory Favorite
소분류 즐겨찾기를 등록/해제합니다.

**Endpoint**: `PATCH /api/v1/subcategories/{subCategoryId}`

**Path Parameters**:
- `subCategoryId` (Long): 소분류 ID

**Query Parameters**:
- `favorite` (boolean): true (등록) / false (해제)

**Success Response**:
- **Status**: `200 OK`
```json
{
  "subCategoryId": 1,
  "favorite": true
}
```

---

## 6. FAQ API (TODO - 미구현)

### 6.1 Create FAQ
새로운 FAQ를 작성합니다.

**Endpoint**: `POST /v1/faq`

**Request Body**:
```json
{
  "subCategoryId": 1,
  "title": "FAQ 제목",
  "content": "FAQ 내용",
  "writerId": 1,
  "writerName": "작성자명",
  "manager": "관리자명"
}
```

**Note**: 현재 구현 중 (TODO)

---

### 6.2 Update FAQ
특정 FAQ를 수정합니다.

**Endpoint**: `PUT /v1/faq/{faqId}`

**Path Parameters**:
- `faqId` (Long): FAQ ID

**Request Body**:
```json
{
  "subCategoryId": 1,
  "title": "수정된 FAQ 제목",
  "content": "수정된 FAQ 내용",
  "lastEditorId": 1,
  "lastEditorName": "수정자명",
  "manager": "관리자명"
}
```

**Note**: 현재 구현 중 (TODO)

---

### 6.3 Delete FAQ
특정 FAQ를 삭제 처리합니다 (소프트 삭제).

**Endpoint**: `DELETE /v1/faq/{faqId}`

**Path Parameters**:
- `faqId` (Long): FAQ ID

**Note**: 현재 구현 중 (TODO)

---

### 6.4 Get FAQ
특정 FAQ의 상세 내용을 조회합니다.

**Endpoint**: `GET /v1/faq/{faqId}`

**Path Parameters**:
- `faqId` (Long): FAQ ID

**Note**: 현재 구현 중 (TODO)

---

### 6.5 Get FAQ List
필터 조건에 따라 FAQ 목록을 조회합니다.

**Endpoint**: `GET /v1/faq/list`

**Query Parameters**:
- `filter` (string, optional): 정렬 또는 필터 조건 (예: "latest", "oldest")

**Note**: 현재 구현 중 (TODO)

---

### 6.6 Search FAQ by Keyword
검색어와 날짜 범위를 이용하여 FAQ를 검색합니다.

**Endpoint**: `GET /v1/faq`

**Query Parameters**:
- `searchkey` (string, optional): 검색 키워드
- `startDate` (string, optional): 검색 시작일 (yyyy-MM-dd)
- `endDate` (string, optional): 검색 종료일 (yyyy-MM-dd)

**Note**: 현재 구현 중 (TODO)

---

### 6.7 Search FAQ by Filter
카테고리, 소분류, 날짜 범위를 조건으로 FAQ를 검색합니다.

**Endpoint**: `GET /v1/faq/filter`

**Query Parameters**:
- `category` (string, optional): 대분류명
- `subcategory` (string, optional): 소분류명
- `startDate` (string, optional): 검색 시작일 (yyyy-MM-dd)
- `endDate` (string, optional): 검색 종료일 (yyyy-MM-dd)

**Note**: 현재 구현 중 (TODO)

---

## Common Error Response Format

모든 API는 다음과 같은 공통 에러 응답 형식을 사용합니다:

```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "errorCode": "ERROR_CODE",
    "errorReason": "상세 에러 원인"
  }
}
```

---

## HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | OK - 요청 성공 |
| 201 | Created - 리소스 생성 성공 |
| 207 | Multi-Status - 일부 성공, 일부 실패 |
| 400 | Bad Request - 잘못된 요청 |
| 401 | Unauthorized - 인증 필요 |
| 403 | Forbidden - 권한 부족 |
| 404 | Not Found - 리소스 없음 |
| 409 | Conflict - 리소스 충돌 |
| 422 | Unprocessable Entity - 모든 작업 실패 |
| 500 | Internal Server Error - 서버 에러 |
