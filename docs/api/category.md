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

## ✅ 카테고리 등록

### Endpoint:
**POST /api/v1/categories**

새로운 category(분류)를 등록하는 API입니다.\
•	동일한 이름의 분류가 이미 존재하는 경우 등록할 수 없습니다.\
•	중복이 아닌 경우 새로운 Category를 생성하고 저장합니다.


### Request Body:
```json
{
    "categoryName": "인사관리"
}
```


### Request Example:\
POST /api/v1/categories



### Response (200 OK):
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

### Endpoint: 
**PUT /api/v1/categories/{categoryId}**

기존 category의 이름을 변경하는 API입니다.

동작 방식은 다음과 같습니다:\
•	전달된 categoryId가 존재하는지 확인합니다.\
•	변경하려는 이름이 이미 존재하는지 확인합니다.\
•	중복이 아닐 경우 category 이름을 변경합니다.


### Request Body:
```json
{
  "categoryName": "학생복지"
}
```


### Request Example:
PUT /api/v1/categories/1



### Response (200 OK):
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

## 🔎 카테고리 전체 조회

### Endpoint: 
**GET /api/v1/categories**

전체 category(분류) 목록을 조회하는 API입니다.

등록되어 있는 모든 카테고리를 조회하며,\
별도의 필터 조건 없이 전체 목록을 반환합니다.


### Request Example:
GET /api/v1/categories


### Response (200 OK):
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
            },
            {
                "categoryId": 3,
                "categoryName": "학생복지"
            }
        ]
    }
}
```

### 응답 필드 설명
***필드명	- 설명***\
categories - 전체 카테고리 목록\
categoryId - 분류 ID\
categoryName - 분류 이름


---

## 🗑️ 카테고리 삭제

### Endpoint: 
**DELETE /api/v1/categories/{categoryId}**

특정 category(분류)를 삭제하는 API입니다.

삭제 조건은 다음과 같습니다:\
•	해당 categoryId가 존재해야 합니다.\
•	해당 카테고리에 연결된 FAQ가 없어야 합니다.\
•	해당 카테고리에 연결된 담당자(ManagerCategory)가 없어야 합니다.

위 조건 중 하나라도 만족하지 않으면 삭제할 수 없습니다.



### Request Example:
DELETE /api/v1/categories/1



### Response (200 OK):
```json
{
    "isSuccess": true,
    "message": "카테고리 삭제 성공",
    "details": null
}
```


---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [FAQ API](./faq.md)
- [DB 스키마 - FAQ/Category](../database/schema/faq.md)
