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

## ✏️담당자 등록

### Endpoint: 
**POST /api/v1/manager**

새로운 담당자를 등록하는 API입니다.

동작 방식은 다음과 같습니다:\
•	전달된 categoryId가 존재하는지 확인합니다.\
•	전달된 teamId가 존재하는지 확인합니다.\
•	같은 이름 + 같은 소속 + 같은 전화번호를 가진 담당자가 이미 존재하면 해당 담당자를 재사용합니다.\
•	존재하지 않으면 새로운 담당자를 생성합니다.\
•	해당 담당자가 이미 해당 카테고리에 등록되어 있다면 등록할 수 없습니다.\
•	중복이 아닐 경우 담당자-카테고리 매핑(ManagerCategory)을 생성합니다.


### Request Body:
```json
{
    "name": "홍길동",
    "teamId": 1,
    "categoryId": 1,
    "phonenum": "01012345678"
}
```

### Request Example:
POST /api/v1/managers



### Response (200 OK):
```json
{
    "isSuccess": true,
    "message": "담당자 등록 성공",
    "details": {
        "managerId": 1,
        "categoryId": 1
    }
}
```

---

## 🔎 담당자 목록 조회

### Endpoint: 
**GET /api/v1/manager**

담당자 목록을 조회하는 API입니다.

다음 조건으로 필터링할 수 있습니다:\
•	카테고리(categoryId)\
•	소속(teamId)\
•	즐겨찾기 여부(favoriteOnly)\
•	담당자 이름 검색(searchName)\

조건을 조합하여 조회할 수 있으며, 필터를 지정하지 않으면 전체 담당자 목록이 조회됩니다.

### Query Parameters:
(**key	/ 설명	/ 타입	/ 필수 여부 /	예시**)\
**categoryId**	/ 카테고리 / ID /	Long /	X /	1\
**teamId** / 소속 / ID /	Long /	X /	2\
**favoriteOnly** /	즐겨찾기한 담당자만 조회 여부 / boolean / X (default=false) / true\
**searchName** / 담당자 이름 검색 / String / X / 홍길동

### Request Example:
GET /api/v1/managers?categoryId=1&teamId=2&favoriteOnly=true&searchName=홍길동

또는 전체 조회:\
GET /api/v1/managers


### Response (200 OK):
```json
{
    "isSuccess": true,
    "message": "카테고리 담당자 목록 조회 성공",
    "details": {
        "managers": [
            {
                "categoryId": 1,
                "categoryName": "인사관리",
                "managerId": 1,
                "managerName": "홍길동",
                "managerFavorite": true,
                "teamId": 2,
                "teamName": "정보운영팀",
                "phonenum": "01012345678"
            },
            {
                "categoryId": 1,
                "categoryName": "인사관리",
                "managerId": 2,
                "managerName": "김철수",
                "managerFavorite": false,
                "teamId": 2,
                "teamName": "정보운영팀",
                "phonenum": "01098765432"
            }
        ]
    }
}
```
---

## ✏️ ️담당자 즐겨찾기 등록 및 해제

### Endpoint:
**PATCH /api/v1/managers/{managerId}/category/{categoryId}**

특정 담당자를 특정 카테고리 기준으로 즐겨찾기 등록 또는 해제하는 API입니다.\
•	favorite=true → 즐겨찾기 등록\
•	favorite=false → 즐겨찾기 해제

담당자와 카테고리 간 매핑(ManagerCategory)이 존재해야 하며,\
존재하지 않는 경우 예외가 발생합니다.

### Query Parameter:
(**key / 설명타입 / 필수 여부 / 예시**)\
(favorite / 즐겨찾기 여부 / boolean / O / true)


### Request Example:
즐겨찾기 등록:\
PATCH /api/v1/managers/1/category/3?favorite=true

즐겨찾기 해제:\
PATCH /api/v1/managers/1/category/3?favorite=false


### Response (200 OK):
즐겨찾기 등록 성공
```json
{
    "isSuccess": true,
    "message": "즐겨찾기 등록 성공",
    "details": {
        "managerId": 1,
        "categoryId": 3,
        "favorite": true
    }
}
```
즐겨찾기 해제 성공
```json
{
    "isSuccess": true,
    "message": "즐겨찾기 해제 성공",
    "details": {
        "managerId": 1,
        "categoryId": 3,
        "favorite": false
    }
}
```


### 응답 필드 설명
managerId -	즐겨찾기 상태가 변경된 담당자 ID\
categoryId - 즐겨찾기 상태가 변경된 카테고리 ID\
favorite -	변경된 즐겨찾기 상태

---
## ✂️ 담당자 삭제

### Endpoint:
**DELETE /api/v1/managers/{managerId}**

특정 담당자를 삭제하는 API입니다.

동작 방식은 다음과 같습니다:\
•	전달된 managerId가 존재하는지 확인합니다.\
•	해당 담당자와 연결된 모든 카테고리 매핑(ManagerCategory)을 먼저 삭제합니다.\
•	이후 담당자(Manager)를 삭제합니다.

즉, 담당자 삭제 시 연관된 카테고리 매핑 정보도 함께 제거됩니다.



### Request Example:
DELETE /api/v1/managers/1


### Response (200 OK):
```json
{
    "isSuccess": true,
    "message": "담당자 삭제 성공",
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
