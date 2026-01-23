# API 참조

## 📑 목차
- [개요](#-개요)
- [Base URL](#base-url)
- [인증 (Authentication)](#-인증-authentication)
- [공통 규칙](#-공통-규칙)
- [API 문서 목록](#-api-문서-목록)
- [빠른 참조](#-빠른-참조)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 백엔드 시스템의 REST API 엔드포인트 문서입니다.
각 도메인별로 API가 분리되어 있으며, 상세 내용은 개별 문서를 참고하세요.

---

## Base URL

```
http://localhost:8080
```

개발 환경에서는 기본적으로 `localhost:8080`을 사용합니다.
운영 환경에서는 실제 도메인으로 변경됩니다.

---

## 🔐 인증 (Authentication)

### 인증 방식
CommuteMate는 **JWT (JSON Web Token)** 기반 인증을 사용합니다.

### 토큰 타입
- **AccessToken**: 실제 API 요청 시 사용. 로그인/갱신 응답 본문에 포함되며 `HttpOnly Cookie(accessToken)`도 함께 설정됩니다.  
  ※ 현재 인증 필터는 **Authorization 헤더만** 검사합니다.
- **RefreshToken**: 토큰 갱신 시 사용. 로그인/갱신 응답 본문에 포함되며 `/api/v1/auth/refresh` 호출 시 `Authorization` 헤더로 전달합니다.

### 헤더 형식
```
Authorization: Bearer <token>
```

### 인증 흐름
1. **로그인**: `/api/v1/auth/login` → AccessToken + RefreshToken 발급
2. **API 요청**: `Authorization: Bearer <AccessToken>` 헤더로 호출
3. **토큰 갱신**: `Authorization: Bearer <RefreshToken>` 헤더로 `/api/v1/auth/refresh` 호출
4. **로그아웃**: `/api/v1/auth/logout` → 토큰 블랙리스트 등록

자세한 내용은 [인증 API 문서](./auth.md)를 참고하세요.

### 인증 적용 범위 (SecurityConfig 기준)
- **인증 필요**: `/api/v1/tasks/**`, `/api/v1/task-templates/**`
- **그 외 경로**: `permitAll`  
※ 일부 API는 `@AuthenticationPrincipal`을 사용하므로, 인증 없이 호출 시 오류가 발생할 수 있습니다.

---

## 📐 공통 규칙

### 날짜/시간 형식
모든 날짜 및 시간은 **ISO-8601** 형식을 따릅니다.

| 타입 | 형식 | 예시 |
|------|------|------|
| **날짜** | `yyyy-MM-dd` | `2026-01-22` |
| **시간** | `HH:mm:ss` | `14:30:00` |
| **날짜+시간** | `yyyy-MM-dd'T'HH:mm:ss` | `2026-01-22T14:30:00` |

### 응답 형식
모든 API 응답은 `Response<T>` 래퍼 형식을 따릅니다.

**성공 응답**
```json
{
  "isSuccess": true,
  "message": "성공 메시지",
  "details": {
    // 응답 데이터
  }
}
```

**실패 응답**
```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "timestamp": "2026-01-22T14:30:00"
  }
}
```

자세한 내용은 [에러 처리 규약](../conventions/error-handling.md)을 참고하세요.

### HTTP 상태 코드

| 상태 코드 | 의미 | 사용 예시 |
|----------|------|----------|
| **200 OK** | 성공 | 조회, 수정, 삭제 성공 |
| **201 Created** | 생성 성공 | 회원가입, 일정 신청 성공 |
| **207 Multi-Status** | 부분 성공 | 일정 일괄 신청 부분 성공 |
| **400 Bad Request** | 잘못된 요청 | 필수 파라미터 누락, 형식 오류 |
| **401 Unauthorized** | 인증 실패 | 토큰 없음, 만료, 유효하지 않음 |
| **403 Forbidden** | 권한 없음 | 관리자 전용 API 일반 사용자 접근 |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 ID |
| **422 Unprocessable Entity** | 비즈니스 로직 실패 | 모든 일정 신청 실패 |
| **500 Internal Server Error** | 서버 오류 | 예상치 못한 에러 |

---

## 📂 API 문서 목록

### 🔐 [인증 API](./auth.md) (`/api/v1/auth`)
사용자 인증 및 토큰 관리 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/send-verification-code` | POST | 이메일 인증번호 발송 |
| `/verify-code` | POST | 인증번호 검증 |
| `/register` | POST | 회원가입 |
| `/login` | POST | 로그인 (토큰 발급) |
| `/logout` | POST | 로그아웃 (토큰 무효화) |
| `/refresh` | POST | 토큰 갱신 |

**바로가기**: [인증 API 상세 →](./auth.md)

---

### 📅 [근무 일정 API](./schedule.md) (`/api/v1/work-schedules`)
사용자 근무 일정 신청, 조회, 수정 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/apply` | POST | 근무 일정 일괄 신청 |
| `/modify` | PATCH | 기존 일정 수정 |
| `/` | GET | 나의 월별 일정 조회 |
| `/history` | GET | 나의 근무 이력 조회 |
| `/{scheduleId}` | GET | 특정 일정 상세 조회 |
| `/{scheduleId}` | DELETE | 일정 취소/삭제 요청 |

**바로가기**: [근무 일정 API 상세 →](./schedule.md)

---

### ⏰ [출퇴근 API](./attendance.md) (`/api/v1/attendance`)
QR 코드 기반 출퇴근 체크 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/qr-token` | GET | QR 토큰 발급 (관리자용) |
| `/check-in` | POST | 출근 체크 |
| `/check-out` | POST | 퇴근 체크 |
| `/today` | GET | 오늘의 출퇴근 기록 조회 |
| `/history` | GET | 특정 날짜 출퇴근 기록 조회 |

**바로가기**: [출퇴근 API 상세 →](./attendance.md)

---

### 🏠 [대시보드 API](./home.md) (`/api/v1/home`, `/api/v1/users`)
홈 화면 및 사용자 정보 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/api/v1/home/work-time` | GET | 오늘의 근무 시간 조회 |
| `/api/v1/home/attendance-status` | GET | 현재 출퇴근 상태 조회 |
| `/api/v1/home/work-summary` | GET | 주간/월간 근무 시간 요약 |
| `/api/v1/users/me` | GET | 내 정보 조회 |
| `/api/v1/users/me/work-time/weekly` | GET | 주간 근무 시간 조회 |
| `/api/v1/users/me/work-time/monthly` | GET | 월간 근무 시간 조회 |

**바로가기**: [대시보드 API 상세 →](./home.md)

---

### 📋 [업무 관리 API](./task.md) (`/api/v1/tasks`, `/api/v1/task-templates`)
업무 생성, 수정, 완료 및 템플릿 관리 API

#### 업무 관리
| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/` | GET | 일별 업무 목록 조회 (정기/비정기 구분) |
| `/{taskId}` | GET | 업무 단건 조회 |
| `/` | POST | 업무 생성 (관리자 전용) |
| `/{taskId}` | PATCH | 업무 정보 수정 |
| `/{taskId}/toggle-complete` | PATCH | 업무 완료 상태 토글 |
| `/{taskId}/complete` | PATCH | 업무 완료 상태 설정 |
| `/{taskId}` | DELETE | 업무 삭제 (관리자 전용) |
| `/batch` | PUT | 업무 일괄 생성/수정 (관리자 전용) |

#### 템플릿 관리
| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/` | GET | 템플릿 목록 조회 |
| `/{templateId}` | GET | 템플릿 상세 조회 |
| `/` | POST | 템플릿 생성 (관리자 전용) |
| `/{templateId}` | PUT | 템플릿 수정 (관리자 전용) |
| `/{templateId}` | DELETE | 템플릿 삭제 (관리자 전용) |
| `/{templateId}/active` | PATCH | 템플릿 활성화/비활성화 (관리자 전용) |
| `/{templateId}/apply` | POST | 템플릿 적용하여 업무 일괄 생성 (관리자 전용) |

**바로가기**: [업무 관리 API 상세 →](./task.md)

---

### 🗂️ [카테고리 API](./category.md) (`/api/v1/categories`)
FAQ 카테고리 관리 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/` | GET | 카테고리 전체 조회 |
| `/` | POST | 카테고리 등록 |
| `/{categoryId}` | PUT | 카테고리 수정 |
| `/{categoryId}` | DELETE | 카테고리 삭제 |
| `/{categoryId}` | PATCH | 즐겨찾기 등록/해제 (`favorite` 쿼리) |

**바로가기**: [카테고리 API 상세 →](./category.md)

---

### ❓ [FAQ API](./faq.md) (`/api/v1/faq`)
FAQ 관리 API (일부 엔드포인트는 구현 진행 중)

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/` | POST | FAQ 등록 |
| `/{faqId}` | PUT | FAQ 수정 |
| `/{faqId}` | DELETE | FAQ 삭제 (TODO) |
| `/{faqId}` | GET | FAQ 상세 조회 (TODO) |
| `/list` | GET | FAQ 목록 조회 (TODO) |
| `/` | GET | FAQ 검색 (키워드/기간, TODO) |
| `/filter` | GET | FAQ 검색 (카테고리/기간, TODO) |

**바로가기**: [FAQ API 상세 →](./faq.md)

---

### 👤 [매니저 API](./manager.md) (`/api/v1/manager`)
매니저 권한 및 담당 카테고리 매핑 관리 API

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/{userId}` | POST | 매니저 권한 등록 |
| `/` | POST | 매니저-카테고리 매핑 등록 |
| `/` | PUT | 매니저-카테고리 매핑 수정 |
| `/categories/{managerId}` | DELETE | 매니저-카테고리 매핑 삭제 |
| `/{managerId}` | DELETE | 매니저 권한 해제 |

**바로가기**: [매니저 API 상세 →](./manager.md)

---

### 👨‍💼 [관리자 근무 일정 API](./admin.md) (`/api/v1/admin/schedule`)
관리자 전용 근무 일정 설정 및 변경 요청 처리 API

#### 근무 일정 관리
| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/monthly-limit` | POST | 월별 최대 동시 근무 인원 설정 |
| `/monthly-limit/{year}/{month}` | GET | 월별 제한 조회 |
| `/monthly-limits` | GET | 모든 월별 제한 조회 |
| `/set-apply-term` | POST | 신청 기간 설정 |
| `/process-change-request` | POST | 변경 요청 승인/거부 |
| `/work-time` | GET | 특정 사용자 근무 시간 조회 |
| `/work-time/summary` | GET | 전체 근무 시간 통계 |
| `/history` | GET | 특정 사용자 근무 이력 |
| `/history/all` | GET | 전체 근무 이력 |
| `/apply-requests` | GET | 승인 대기 중인 신청 목록 |

**바로가기**: [관리자 API 상세 →](./admin.md)

---

## 🔍 빠른 참조

### 자주 사용하는 API

| 기능 | 엔드포인트 | 문서 |
|------|----------|------|
| **로그인** | `POST /api/v1/auth/login` | [auth.md#로그인](./auth.md#14-login) |
| **근무 일정 신청** | `POST /api/v1/work-schedules/apply` | [schedule.md#일정-신청](./schedule.md#211-apply-work-schedule) |
| **나의 일정 조회** | `GET /api/v1/work-schedules?year={year}&month={month}` | [schedule.md#일정-조회](./schedule.md#213-get-my-schedules) |
| **출근 체크** | `POST /api/v1/attendance/check-in` | [attendance.md#출근](./attendance.md#32-check-in) |
| **퇴근 체크** | `POST /api/v1/attendance/check-out` | [attendance.md#퇴근](./attendance.md#33-check-out) |
| **홈 화면 데이터** | `GET /api/v1/home/work-summary` | [home.md#근무-요약](./home.md#53-get-work-summary) |
| **월별 제한 설정** | `POST /api/v1/admin/schedule/monthly-limit` | [admin.md](./admin.md) |

### CodeType 참조

API에서 사용하는 코드 값들:

| 코드 분류 | 코드 값 | 설명 | 관련 API |
|----------|--------|------|----------|
| **역할 (RL)** | `RL01` | 학생/사원 | 회원가입 |
| | `RL02` | 관리자 | 회원가입 |
| **근무 상태 (WS)** | `WS01` | 신청됨 | 근무 일정 |
| | `WS02` | 승인됨 | 근무 일정 |
| | `WS03` | 거부됨 | 근무 일정 |
| | `WS04` | 취소됨 | 근무 일정 |
| **변경 요청 타입 (CR)** | `CR01` | 수정 | 변경 요청 |
| | `CR02` | 삭제 | 변경 요청 |
| **변경 요청 상태 (CS)** | `CS01` | 대기 | 변경 요청 |
| | `CS02` | 승인 | 변경 요청 |
| | `CS03` | 거부 | 변경 요청 |
| **체크 타입 (CT)** | `CT01` | 출근 | 출퇴근 |
| | `CT02` | 퇴근 | 출퇴근 |
| **업무 타입 (TT)** | `TT01` | 정기 업무 | 업무 관리 |
| | `TT02` | 비정기 업무 | 업무 관리 |

자세한 내용은 [코드 시스템 문서](../database/schema/code-system.md)를 참고하세요.

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [에러 처리 규약](../conventions/error-handling.md) - API 에러 응답 표준
- **필수**: [API 설계 규약](../conventions/api-conventions.md) - Request/Response 설계 원칙
- **참고**: [코드 시스템](../database/schema/code-system.md) - CodeType Enum 상세
- **참고**: [아키텍처 개요](../architecture/overview.md) - API 계층 구조

### 상위/하위 문서
- ⬆️ **상위**: [문서 홈](../README.md)
- ⬇️ **하위**:
  - [인증 API](./auth.md)
  - [근무 일정 API](./schedule.md)
  - [출퇴근 API](./attendance.md)
  - [대시보드 API](./home.md)
  - [업무 관리 API](./task.md)
  - [카테고리 API](./category.md)
  - [FAQ API](./faq.md)
  - [매니저 API](./manager.md)
  - [관리자 API](./admin.md)
