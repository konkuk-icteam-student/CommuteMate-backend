# 카테고리 API (Category)

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [카테고리 등록](#-카테고리-등록)
- [카테고리 수정](#-카테고리-수정)
- [카테고리 전체 조회](#-카테고리-전체-조회)
- [카테고리 삭제](#-카테고리-삭제)
- [즐겨찾기 등록/해제](#-즐겨찾기-등록해제)
- [관련 문서](#-관련-문서)

---

## 📖 개요

FAQ 분류용 카테고리를 관리하는 API입니다.

**Base Path**: `/api/v1/categories`

---

## 🔐 인증

현재 `SecurityConfig` 기준으로 인증이 강제되지 않습니다. (permitAll)

---

## ✅ 카테고리 등록

**Endpoint**: `POST /api/v1/categories`

**Request Body**:
```json
{
  "categoryName": "인사관리"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "카테고리 등록 성공",
  "details": {
    "categoryId": 1
  }
}
```

---

## ✏️ 카테고리 수정

**Endpoint**: `PUT /api/v1/categories/{categoryId}`

**Request Body**:
```json
{
  "categoryName": "학생복지"
}
```

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "카테고리 수정 성공",
  "details": {
    "categoryId": 1,
    "updatedName": "학생복지"
  }
}
```

---

## 📋 카테고리 전체 조회

**Endpoint**: `GET /api/v1/categories`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "전체 카테고리 조회 성공",
  "details": {
    "categories": [
      {
        "categoryId": 1,
        "categoryName": "시스템"
      },
      {
        "categoryId": 2,
        "categoryName": "인사관리"
      }
    ]
  }
}
```

---

## 🗑️ 카테고리 삭제

**Endpoint**: `DELETE /api/v1/categories/{categoryId}`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "성공적으로 삭제되었습니다.",
  "details": null
}
```

---

## ⭐ 즐겨찾기 등록/해제

**Endpoint**: `PATCH /api/v1/categories/{categoryId}?favorite=true|false`

**Response (200 OK)**:
```json
{
  "isSuccess": true,
  "message": "즐겨찾기 등록 성공",
  "details": {
    "categoryId": 1,
    "favorite": true
  }
}
```

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [FAQ API](./faq.md)
- [DB 스키마 - FAQ/Category](../database/schema/faq.md)
