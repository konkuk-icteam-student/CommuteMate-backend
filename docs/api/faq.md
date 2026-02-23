# FAQ API

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [FAQ 등록](#-faq-등록)
- [FAQ 수정](#-faq-수정)
- [FAQ 삭제 (TODO)](#-faq-삭제-todo)
- [FAQ 상세 조회 (TODO)](#-faq-상세-조회-todo)
- [FAQ 목록/검색 (TODO)](#-faq-목록검색-todo)
- [관련 문서](#-관련-문서)

---

## 📖 개요

FAQ 작성/수정 및 조회를 위한 API입니다. 일부 조회 기능은 아직 구현되지 않았습니다.

**Base Path**: `/api/v1/faq`

---

## 🔐 인증

현재 `SecurityConfig` 기준으로 인증이 강제되지 않습니다. (permitAll)

※ 임시 구현으로 **요청 바디에 userId가 포함**됩니다. (인증 로직 안정화 후 제거 예정)

---

## ✅ FAQ 등록

**Endpoint**: `POST /api/v1/faq`

**Request Body**:
```json
{
  "userId": 1,
  "category": "인사관리",
  "title": "학정시 로그인 오류",
  "content": "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸",
  "attachmentUrl": "test.pdf",
  "etc": "없음"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "FAQ 작성 성공",
  "details": {
    "faqId": 1
  }
}
```

---

## ✏️ FAQ 수정

**Endpoint**: `PUT /api/v1/faq/{faqId}`

**Request Body**:
```json
{
  "userId": 1,
  "title": "인사관리",
  "content": "학정시 로그인을 하려는데 OTP 관련 메시지가 뜸",
  "etc": "없음",
  "attachmentUrl": "test.pdf",
  "manager": "김철수"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "FAQ 수정 성공",
  "details": {
    "faqId": 1
  }
}
```

---

## 🗑️ FAQ 삭제 (TODO)

**Endpoint**: `DELETE /api/v1/faq/{faqId}`

특정 FAQ를 삭제 처리하는 API입니다.
실제 데이터는 삭제하지 않고 deleted_flag를 true로 변경하며, deleted_at에 삭제 시간을 기록하는 Soft Delete 방식을 사용합니다.

Soft Delete는 데이터를 물리적으로 삭제하지 않고 삭제 여부만 표시하는 방식으로,
과거 문서 보존을 위해 사용됩니다.
프론트에서는 삭제된 FAQ에 대해 “삭제됨” 배지를 표시합니다.

**Request Example**:
DELETE /api/v1/faq/1

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "FAQ 삭제 성공",
  "details": null
}
```
---

## 🔎 FAQ 상세 조회 (TODO)

**Endpoint**: `GET /api/v1/faq/{faqId}`

특정 FAQ를 조회 날짜 기준으로 상세 조회하는 API입니다.
같은 FAQ라도 수정 이력이 존재하는 경우, date 파라미터를 기준으로 해당 시점의 이력(FaqHistory)을 조회합니다.

삭제된 FAQ도 조회 가능하며, deletedFlag 및 deletedAt 값을 통해 삭제 여부를 확인할 수 있습니다.

**Request Example**:
GET /api/v1/faq/1?date=2026-01-01

**Response (200 OK)**:
```json
{
    "isSuccess": true,
    "message": "FAQ 상세 조회 성공",
    "details": {
        "faqId": 1,
        "title": "학정시 로그인 오류",
        "categoryName": "로그인",
        "deletedFlag": false,
        "complainantName": "홍길동",
        "writerName": "양지윤",
        "answer": "비밀번호 재설정 후 다시 로그인해주세요.",
        "etc": "반복 문의 발생",
        "pastManagers": [
            {
            "managerName": "김철수",
            "teamName": "인사팀",
            "categoryName": "로그인"
            }
        ],
        "currentManagers": [
            {
            "managerName": "이영희",
            "teamName": "고객지원팀",
            "categoryName": "로그인"
            }
        ],
        "editedDates": [
            "2024-11-01",
            "2024-11-10"
        ],
        "deletedAt": null
    }
}
```

---

## 📋 FAQ 목록/검색 (TODO)

**Endpoint**:
- **목록 조회**: `GET /api/v1/faq/list?filter=latest`
- **키워드 검색**: `GET /api/v1/faq?searchkey=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
- **필터 검색**: `GET /api/v1/faq/filter?category=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`

필터 조건에 따라 FAQ 목록을 조회하는 API입니다.

다음과 같은 조건으로 필터링할 수 있습니다:\
•	소속(teamId)\
•	분류(categoryId)\
•	검색어(keyword)\
•	검색 범위(searchScope)\
•	날짜 범위(startDate ~ endDate)

기본 정렬은 최신순이며,
페이지 단위로 조회하고 페이지당 10개씩 조회됩니다.
페이지 번호는 0부터 시작합니다.

**Request Example**:
GET /api/v1/faq?teamId=1&categoryId=3&keyword=로그인&searchScope=TITLE&page=0

**Response (200 OK)**:
```json
{
    "isSuccess": true,
    "message": "FAQ 목록 조회 성공",
    "details": {
        "faqs": [
            {
                "faqId": 1,
                "title": "학정시 로그인 오류",
                "updatedDate": "2025-01-25",
                "deletedFlag": false
            },
            {
                "faqId": 2,
                "title": "비밀번호 재설정 방법",
                "updatedDate": "2025-01-20",
                "deletedFlag": true
            }
        ],
        "page": 0,
        "totalPages": 5
    }
}
```
---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [카테고리 API](./category.md)
- [DB 스키마 - FAQ/Category](../database/schema/faq.md)
