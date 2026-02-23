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
삭제 이력 관리위해 사용됩니다.
프론트에서는 삭제된 FAQ에 대해 “삭제됨” 배지를 표시합니다.
---
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

현재 컨트롤러가 `TODO` 상태로, 실제 응답 로직이 구현되어 있지 않습니다.

---

## 📋 FAQ 목록/검색 (TODO)

- **목록 조회**: `GET /api/v1/faq/list?filter=latest`
- **키워드 검색**: `GET /api/v1/faq?searchkey=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`
- **필터 검색**: `GET /api/v1/faq/filter?category=...&startDate=yyyy-MM-dd&endDate=yyyy-MM-dd`

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [카테고리 API](./category.md)
- [DB 스키마 - FAQ/Category](../database/schema/faq.md)
