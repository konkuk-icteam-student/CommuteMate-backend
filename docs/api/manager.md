# 매니저 API (Manager)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [매니저 권한 등록](#-매니저-권한-등록)
- [매니저-카테고리 매핑 등록](#-매니저-카테고리-매핑-등록)
- [매니저-카테고리 매핑 수정](#-매니저-카테고리-매핑-수정)
- [매니저-카테고리 매핑 삭제](#-매니저-카테고리-매핑-삭제)
- [매니저 권한 해제](#-매니저-권한-해제)
- [관련 문서](#-관련-문서)

---

## 📖 개요

담당자(매니저) 권한 부여 및 카테고리 매핑을 관리하는 API입니다.

**Base Path**: `/api/v1/manager`

---

## 🔐 인증

현재 `SecurityConfig` 기준으로 인증이 강제되지 않습니다. (permitAll)

---

## ✅ 매니저 권한 등록

**Endpoint**: `POST /api/v1/manager/{userId}`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "매니저 등록 성공",
  "details": null
}
```

---

## ✅ 매니저-카테고리 매핑 등록

**Endpoint**: `POST /api/v1/manager`

**Request Body**:
```json
{
  "managerId": 3,
  "categoryIds": [1, 2, 3]
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "manager-category 매핑 등록 성공",
  "details": {
    "count": 3
  }
}
```

---

## ✏️ 매니저-카테고리 매핑 수정

**Endpoint**: `PUT /api/v1/manager`

**Request Body**:
```json
{
  "managerId": 5,
  "categoryNames": ["도서관시스템", "학사정보시스템"]
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "manager-category 매핑이 성공적으로 수정되었습니다.",
  "details": null
}
```

---

## 🗑️ 매니저-카테고리 매핑 삭제

**Endpoint**: `DELETE /api/v1/manager/categories/{managerId}`

현재 구현은 `managerId`만 받아 전체 매핑을 삭제합니다. (세부 매핑 선택은 TODO)

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "manager-category 매핑이 정상적으로 삭제되었습니다.",
  "details": null
}
```

---

## 🧹 매니저 권한 해제

**Endpoint**: `DELETE /api/v1/manager/{managerId}`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "manager 권한이 해제되었습니다.",
  "details": null
}
```

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [카테고리 API](./category.md)
- [DB 스키마 - FAQ/Category](../database/schema/faq.md)
