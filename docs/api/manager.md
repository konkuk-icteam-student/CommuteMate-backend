# 담당자 API (Manager)

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

담당자(매니저) 관리 및 카테고리 기반 담당자 조회 API입니다.

조직 내 다양한 카테고리(도서관시스템, 학사정보시스템 등)의 담당자를 등록하고 조회할 수 있습니다.

**Base Path**: `/api/v1/manager`

**태그**: `Manager`

---

## 🔐 인증

| 엔드포인트 | 인증 필요 | 설명 |
|-----------|---------|------|
| `POST /manager` | ❌ 아니오 | 담당자 등록 (현재 공개) |
| `GET /manager` | ❌ 아니오 | 담당자 목록 조회 (현재 공개) |

**주의**: 현재 `SecurityConfig` 기준으로 인증이 강제되지 않습니다. (permitAll)

향후 보안 강화를 위해 관리자 권한 검증이 필요할 수 있습니다.

---

## 🎯 주요 엔드포인트

| 메서드 | 경로 | 설명 | HTTP 상태 | 인증 |
|--------|------|------|----------|------|
| POST | `/` | 담당자 등록 | 200 | ❌ |
| GET | `/` | 담당자 목록 조회 (필터링 가능) | 200 | ❌ |

---

## 📋 상세 엔드포인트 문서

### 1️⃣ POST `/api/v1/manager` - 담당자 등록

**설명**: 새로운 담당자를 등록합니다.

담당자는 카테고리별로 관리되며, 이미 등록된 담당자는 재등록할 수 없습니다.

**Request**

```bash
curl -X POST http://localhost:8080/api/v1/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerId": 5,
    "categoryIds": [1, 2, 3]
  }'
```

**Request Body Schema**:

```json
{
  "managerId": 5,
  "categoryIds": [1, 2, 3]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| managerId | Long | ✅ | 담당자의 사용자 ID |
| categoryIds | Array[Long] | ✅ | 담당 카테고리 ID 목록 |

**Response 200 OK** - 등록 성공

```json
{
  "isSuccess": true,
  "message": "담당자 등록 성공",
  "details": {
    "managerId": 5,
    "categories": [
      {
        "categoryId": 1,
        "categoryName": "도서관시스템"
      },
      {
        "categoryId": 2,
        "categoryName": "학사정보시스템"
      },
      {
        "categoryId": 3,
        "categoryName": "기숙사시스템"
      }
    ],
    "registeredAt": "2025-01-24T14:30:00"
  }
}
```

**응답 필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| managerId | Long | 등록된 담당자 ID |
| categories | Array | 할당된 카테고리 목록 |
| categories[].categoryId | Long | 카테고리 ID |
| categories[].categoryName | String | 카테고리 이름 |
| registeredAt | DateTime | 등록 일시 |

**에러 응답**

**400 Bad Request** - 잘못된 요청

```json
{
  "isSuccess": false,
  "message": "요청 데이터가 유효하지 않습니다.",
  "details": null
}
```

**404 Not Found** - 해당 카테고리 없음

```json
{
  "isSuccess": false,
  "message": "존재하지 않는 카테고리입니다.",
  "details": {
    "categoryId": 999,
    "reason": "카테고리 ID 999가 DB에 없습니다."
  }
}
```

**409 Conflict** - 이미 등록된 담당자

```json
{
  "isSuccess": false,
  "message": "이미 등록된 담당자입니다.",
  "details": {
    "managerId": 5,
    "categoryId": 1,
    "reason": "담당자 5는 이미 카테고리 1의 담당자로 등록되어 있습니다."
  }
}
```

**500 Internal Server Error** - 서버 오류

```json
{
  "isSuccess": false,
  "message": "담당자 등록 중 오류가 발생했습니다.",
  "details": null
}
```

---

### 2️⃣ GET `/api/v1/manager` - 담당자 목록 조회

**설명**: 담당자 목록을 조회합니다.

카테고리, 팀(조직), 즐겨찾기 여부로 필터링할 수 있습니다.

**Request**

```bash
# 모든 담당자 조회
curl -X GET "http://localhost:8080/api/v1/manager" \
  -H "Content-Type: application/json"

# 특정 카테고리의 담당자만 조회
curl -X GET "http://localhost:8080/api/v1/manager?categoryId=1" \
  -H "Content-Type: application/json"

# 특정 팀의 담당자만 조회
curl -X GET "http://localhost:8080/api/v1/manager?team=IT부서" \
  -H "Content-Type: application/json"

# 즐겨찾기한 담당자만 조회
curl -X GET "http://localhost:8080/api/v1/manager?favoriteOnly=true" \
  -H "Content-Type: application/json"

# 조건 조합: 특정 카테고리의 즐겨찾기 담당자
curl -X GET "http://localhost:8080/api/v1/manager?categoryId=1&favoriteOnly=true" \
  -H "Content-Type: application/json"
```

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| categoryId | Long | ❌ | - | 카테고리 ID (지정 시 해당 카테고리의 담당자만 조회) |
| team | String | ❌ | - | 팀/조직명 (지정 시 해당 팀의 담당자만 조회) |
| favoriteOnly | Boolean | ❌ | false | true 시 즐겨찾기한 담당자만 조회 |

**Response 200 OK** - 조회 성공

```json
{
  "isSuccess": true,
  "message": "카테고리 담당자 목록 조회 성공",
  "details": {
    "totalCount": 3,
    "managers": [
      {
        "managerId": 1,
        "managerName": "이순신",
        "email": "lee@example.com",
        "phone": "010-1234-5678",
        "team": "IT부서",
        "categories": [
          {
            "categoryId": 1,
            "categoryName": "도서관시스템"
          },
          {
            "categoryId": 2,
            "categoryName": "학사정보시스템"
          }
        ],
        "isFavorite": true,
        "registeredAt": "2025-01-10T09:00:00"
      },
      {
        "managerId": 2,
        "managerName": "김유신",
        "email": "kim@example.com",
        "phone": "010-2345-6789",
        "team": "인프라팀",
        "categories": [
          {
            "categoryId": 3,
            "categoryName": "기숙사시스템"
          }
        ],
        "isFavorite": false,
        "registeredAt": "2025-01-12T10:30:00"
      },
      {
        "managerId": 5,
        "managerName": "장보고",
        "email": "jang@example.com",
        "phone": "010-3456-7890",
        "team": "IT부서",
        "categories": [
          {
            "categoryId": 1,
            "categoryName": "도서관시스템"
          }
        ],
        "isFavorite": true,
        "registeredAt": "2025-01-15T14:20:00"
      }
    ]
  }
}
```

**응답 필드 설명**:

| 필드 | 타입 | 설명 |
|------|------|------|
| totalCount | Integer | 조회된 담당자 총 개수 |
| managers | Array | 담당자 정보 배열 |
| managers[].managerId | Long | 담당자 ID |
| managers[].managerName | String | 담당자 이름 |
| managers[].email | String | 담당자 이메일 |
| managers[].phone | String | 담당자 휴대폰 번호 |
| managers[].team | String | 소속 팀/조직명 |
| managers[].categories | Array | 담당 카테고리 목록 |
| managers[].isFavorite | Boolean | 즐겨찾기 여부 |
| managers[].registeredAt | DateTime | 등록 일시 |

**에러 응답**

**400 Bad Request** - 잘못된 쿼리 파라미터

```json
{
  "isSuccess": false,
  "message": "요청 파라미터가 유효하지 않습니다.",
  "details": {
    "invalidParam": "categoryId",
    "reason": "categoryId는 양수여야 합니다."
  }
}
```

**500 Internal Server Error** - 서버 오류

```json
{
  "isSuccess": false,
  "message": "담당자 목록 조회 중 오류가 발생했습니다.",
  "details": null
}
```

---

## 🚨 에러 처리

### HTTP 상태 코드 매핑

| HTTP 상태 | 에러 | 설명 |
|----------|------|------|
| **200** | Success | 요청 성공 |
| **400** | Bad Request | 잘못된 요청 데이터 |
| **404** | Not Found | 해당 리소스 없음 (카테고리 등) |
| **409** | Conflict | 중복된 등록 (이미 존재) |
| **500** | Internal Server Error | 서버 처리 중 오류 발생 |

### 공통 에러 시나리오

**1. 유효하지 않은 카테고리 ID**

```bash
curl -X POST http://localhost:8080/api/v1/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerId": 5,
    "categoryIds": [999]
  }'
```

**응답 (404 Not Found)**:

```json
{
  "isSuccess": false,
  "message": "존재하지 않는 카테고리입니다.",
  "details": {
    "categoryId": 999
  }
}
```

**해결 방법**: 유효한 카테고리 ID 확인 (카테고리 API 참고)

---

**2. 이미 등록된 담당자**

```bash
curl -X POST http://localhost:8080/api/v1/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerId": 5,
    "categoryIds": [1]
  }'
```

*담당자 5는 이미 카테고리 1의 담당자로 등록됨*

**응답 (409 Conflict)**:

```json
{
  "isSuccess": false,
  "message": "이미 등록된 담당자입니다.",
  "details": {
    "managerId": 5,
    "categoryId": 1
  }
}
```

**해결 방법**: 다른 카테고리 선택 또는 기존 담당자 확인

---

**3. 빈 요청 바디**

```bash
curl -X POST http://localhost:8080/api/v1/manager \
  -H "Content-Type: application/json" \
  -d '{}'
```

**응답 (400 Bad Request)**:

```json
{
  "isSuccess": false,
  "message": "요청 데이터가 유효하지 않습니다.",
  "details": {
    "reason": "managerId와 categoryIds는 필수입니다."
  }
}
```

**해결 방법**: 필수 필드 모두 입력

---

## 📚 사용 예시

### 예시 1: 새로운 담당자 등록

**시나리오**: 새로운 담당자를 도서관시스템과 학사정보시스템 카테고리로 등록

```bash
#!/bin/bash

curl -X POST http://localhost:8080/api/v1/manager \
  -H "Content-Type: application/json" \
  -d '{
    "managerId": 5,
    "categoryIds": [1, 2]
  }' | jq '.'
```

**예상 응답**:

```json
{
  "isSuccess": true,
  "message": "담당자 등록 성공",
  "details": {
    "managerId": 5,
    "categories": [
      {
        "categoryId": 1,
        "categoryName": "도서관시스템"
      },
      {
        "categoryId": 2,
        "categoryName": "학사정보시스템"
      }
    ]
  }
}
```

---

### 예시 2: 모든 담당자 목록 조회

**시나리오**: 현재 등록된 모든 담당자의 목록 확인

```bash
#!/bin/bash

curl -X GET "http://localhost:8080/api/v1/manager" \
  -H "Content-Type: application/json" | jq '.details.managers[] | {managerId, managerName, team, categories}'
```

**예상 응답**:

```json
{
  "managerId": 1,
  "managerName": "이순신",
  "team": "IT부서",
  "categories": [
    {
      "categoryId": 1,
      "categoryName": "도서관시스템"
    }
  ]
}
{
  "managerId": 2,
  "managerName": "김유신",
  "team": "인프라팀",
  "categories": [
    {
      "categoryId": 3,
      "categoryName": "기숙사시스템"
    }
  ]
}
```

---

### 예시 3: 특정 카테고리의 담당자 조회

**시나리오**: 도서관시스템(categoryId=1) 카테고리의 담당자만 조회

```bash
#!/bin/bash

curl -X GET "http://localhost:8080/api/v1/manager?categoryId=1" \
  -H "Content-Type: application/json" | jq '.details | {totalCount, managers}'
```

**예상 응답**:

```json
{
  "totalCount": 2,
  "managers": [
    {
      "managerId": 1,
      "managerName": "이순신",
      "email": "lee@example.com",
      "team": "IT부서",
      "categories": [
        {
          "categoryId": 1,
          "categoryName": "도서관시스템"
        }
      ]
    },
    {
      "managerId": 5,
      "managerName": "장보고",
      "email": "jang@example.com",
      "team": "IT부서",
      "categories": [
        {
          "categoryId": 1,
          "categoryName": "도서관시스템"
        }
      ]
    }
  ]
}
```

---

### 예시 4: 즐겨찾기 담당자만 조회

**시나리오**: 사용자가 즐겨찾기한 담당자만 빠르게 조회

```bash
#!/bin/bash

curl -X GET "http://localhost:8080/api/v1/manager?favoriteOnly=true" \
  -H "Content-Type: application/json" | jq '.details.managers[] | {managerName, email, isFavorite}'
```

**예상 응답**:

```json
{
  "managerName": "이순신",
  "email": "lee@example.com",
  "isFavorite": true
}
{
  "managerName": "장보고",
  "email": "jang@example.com",
  "isFavorite": true
}
```

---

### 예시 5: TypeScript 클라이언트에서 호출

**시나리오**: 프론트엔드에서 담당자를 등록하고 목록을 조회

```typescript
// manager.service.ts

interface PostManagerRequest {
  managerId: number;
  categoryIds: number[];
}

async function registerManager(request: PostManagerRequest): Promise<any> {
  const response = await fetch('http://localhost:8080/api/v1/manager', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  const data = await response.json();

  if (!data.isSuccess) {
    throw new Error(data.message);
  }

  return data.details;
}

async function getManagerList(
  categoryId?: number,
  team?: string,
  favoriteOnly: boolean = false
): Promise<any[]> {
  const params = new URLSearchParams();

  if (categoryId) params.append('categoryId', categoryId.toString());
  if (team) params.append('team', team);
  params.append('favoriteOnly', favoriteOnly.toString());

  const response = await fetch(
    `http://localhost:8080/api/v1/manager?${params}`,
    {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  const data = await response.json();

  if (!data.isSuccess) {
    throw new Error(data.message);
  }

  return data.details.managers;
}

// 사용 예
async function handleManagerRegistration() {
  try {
    // 담당자 등록
    const result = await registerManager({
      managerId: 5,
      categoryIds: [1, 2]
    });
    console.log('담당자 등록 완료:', result);

    // 목록 조회
    const managers = await getManagerList();
    console.log('전체 담당자:', managers);

    // 카테고리별 조회
    const libraryManagers = await getManagerList(1);
    console.log('도서관시스템 담당자:', libraryManagers);
  } catch (error) {
    console.error('담당자 관리 오류:', error);
  }
}
```

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [카테고리 API](./category.md)
- [FAQ API](./faq.md)
- [사용자 API](./user.md)
- [데이터베이스 스키마 - FAQ/Category](../database/schema/faq.md)
- [전체 엔드포인트 요약](./endpoints-summary.md)
