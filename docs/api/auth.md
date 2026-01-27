# 인증 API

## 📑 목차
- [개요](#-개요)
- [Base Path](#base-path)
- [인증 흐름](#-인증-흐름)
- [API 엔드포인트](#-api-엔드포인트)
  - [1.1 이메일 인증번호 발송](#11-send-verification-code)
  - [1.2 인증번호 검증](#12-verify-code)
  - [1.3 회원가입](#13-register)
  - [1.4 로그인](#14-login)
  - [1.5 로그아웃](#15-logout)
  - [1.6 토큰 갱신](#16-refresh-token)
- [에러 처리](#-에러-처리)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 인증 API는 **JWT (JSON Web Token)** 기반 인증을 제공합니다.
이메일 인증 → 회원가입 → 로그인 → 토큰 관리의 전체 인증 흐름을 지원합니다.

---

## Base Path

```
/api/v1/auth
```

---

## 🔐 인증 흐름

```
[회원가입 흐름]
1. POST /send-verification-code  → 이메일로 인증번호 발송
2. POST /verify-code             → 인증번호 검증
3. POST /register                → 회원가입 완료

[로그인/토큰 관리 흐름]
4. POST /login                   → AccessToken + RefreshToken 발급
5. API 요청 (`Authorization: Bearer <AccessToken>` 헤더)
6. POST /refresh                 → AccessToken 만료 시 갱신
7. POST /logout                  → 토큰 무효화 (블랙리스트)
```

---

## 📚 API 엔드포인트

### 1.1 Send Verification Code

이메일로 인증번호를 발송합니다.

**Endpoint**
```
POST /api/v1/auth/send-verification-code
```

**Request Body**
```json
{
  "email": "user@example.com"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 이메일 주소 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "인증번호가 이메일로 발송되었습니다. (유효시간: 5분)",
  "details": null
}
```

**에러 응답**
- `400 Bad Request`: 이메일 형식 오류
- `409 Conflict`: 이미 가입된 이메일

**관련 엔티티**: [User 테이블](../database/schema/user.md#user-테이블)

---

### 1.2 Verify Code

발송된 인증번호를 검증합니다.

**Endpoint**
```
POST /api/v1/auth/verify-code
```

**Request Body**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 이메일 주소 |
| code | String | ✅ | 6자리 인증번호 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.",
  "details": null
}
```

**에러 응답**
- `400 Bad Request`: 인증번호 형식 오류
- `401 Unauthorized`: 인증번호 불일치 또는 만료

---

### 1.3 Register

회원가입을 진행합니다.

**Endpoint**
```
POST /api/v1/auth/register
```

**Request Body**
```json
{
  "email": "newuser@example.com",
  "password": "securePass123",
  "name": "홍길동",
  "roleCode": "RL01",
  "organizationId": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 이메일 주소 (인증 완료된 이메일) |
| password | String | ✅ | 비밀번호 (4자 이상 16자 이하, 영문/숫자만) |
| name | String | ✅ | 사용자 이름 |
| roleCode | String | ✅ | 역할 코드 (`RL01`: 학생/사원, `RL02`: 관리자) |
| organizationId | Long | ✅ | 소속 조직 ID |

**roleCode 값**
- `RL01`: 학생/사원 (일반 사용자)
- `RL02`: 관리자

자세한 내용은 [CodeType 문서](../database/schema/code-system.md#rl-역할)를 참고하세요.

**Response (201 Created)**
```json
{
  "isSuccess": true,
  "message": "회원가입이 완료되었습니다.",
  "details": {
    "userId": 1,
    "email": "newuser@example.com",
    "name": "홍길동",
    "roleCode": "RL01"
  }
}
```

**에러 응답**
- `400 Bad Request`: 필수 필드 누락 또는 형식 오류
- `401 Unauthorized`: 이메일 인증 미완료
- `409 Conflict`: 이미 가입된 이메일

**관련 엔티티**:
- [User 테이블](../database/schema/user.md#user-테이블)
- [Organization 테이블](../database/schema/user.md#organization-테이블)
- [CodeType.RL](../database/schema/code-system.md#rl-역할)

---

### 1.4 Login

로그인하여 JWT 토큰을 발급받습니다.

**Endpoint**
```
POST /api/v1/auth/login
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | String | ✅ | 이메일 주소 |
| password | String | ✅ | 비밀번호 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "로그인 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresAt": 1736560000000,
    "userId": 1,
    "userName": "홍길동",
    "email": "user@example.com",
    "roleCode": "RL01"
  }
}
```

**Response Fields**
| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | String | API 요청용 액세스 토큰 (응답 본문 + `HttpOnly Cookie`로도 설정됨) |
| refreshToken | String | 토큰 갱신용 리프레시 토큰 (DB 저장 + 클라이언트 저장 필요) |
| tokenType | String | 토큰 타입 (항상 `Bearer`) |
| expiresAt | Long | AccessToken 만료 시간 (Unix Timestamp, **ms 단위**) |
| userId | Long | 사용자 ID |
| userName | String | 사용자 이름 |
| email | String | 사용자 이메일 |
| roleCode | String | 역할 코드 (예: "RL01") |

**에러 응답**
- `400 Bad Request`: 필수 필드 누락
- `401 Unauthorized`: 이메일 또는 비밀번호 불일치

**토큰 사용 방법**
- **AccessToken**: `Authorization: Bearer <AccessToken>` 헤더로 전달 (쿠키는 서버 인증 필터에서 사용하지 않음)
- **RefreshToken**: 클라이언트에 저장 후 `/api/v1/auth/refresh` 호출 시 `Authorization` 헤더로 전달

**관련 문서**: [JWT 인증 구조](../conventions/api-conventions.md#jwt-인증)

---

### 1.5 Logout

로그아웃하여 토큰을 무효화합니다.

**Endpoint**
```
POST /api/v1/auth/logout
```

**Headers**
```
Authorization: Bearer <accessToken>   // 선택 사항 (없어도 200 응답)
```

**Request Body**: 없음

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "로그아웃되었습니다.",
  "details": null
}
```

**처리 과정**
1. AccessToken이 전달되면 블랙리스트에 등록
2. 해당 토큰은 이후 인증 처리에서 거부됨
3. 클라이언트는 저장된 RefreshToken도 삭제해야 함

**에러 응답**
- 현재 구현상 토큰 유효성 검증을 하지 않으므로 별도 에러 응답 없음

**관련 서비스**: `TokenBlacklistService` (auth/application/)

---

### 1.6 Refresh Token

리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.

**Endpoint**
```
POST /api/v1/auth/refresh
```

**Headers**
```
Authorization: Bearer <refreshToken>
```

**Request Body**: 없음

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "message": "토큰 갱신 성공",
  "details": {
    "accessToken": "eyJhbGciOiJIUzI1Ni...",
    "refreshToken": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresAt": 1736560000000,
    "userId": 1,
    "userName": "홍길동",
    "email": "user@example.com",
    "roleCode": "RL01"
  }
}
```

**Response Fields**: [로그인 응답](#14-login)과 동일 (userId, userName, email, roleCode 포함)

**에러 응답**
```json
{
  "isSuccess": false,
  "message": "유효하지 않은 리프레시 토큰입니다.",
  "details": {
    "timestamp": "2026-01-22T14:30:00"
  }
}
```

**토큰 갱신 시나리오**
1. 클라이언트가 API 요청 시 `401 Unauthorized` 수신
2. 저장된 RefreshToken으로 `/api/v1/auth/refresh` 호출
3. 새로운 AccessToken + RefreshToken 수신
4. 실패한 API 요청 재시도

**관련 문서**: [에러 처리 규약](../conventions/error-handling.md)

---

## ⚠️ 에러 처리

### 공통 에러 코드

| HTTP 상태 | errorCode | 설명 | 해결 방법 |
|----------|-----------|------|----------|
| **400** | `INVALID_VERIFICATION_CODE` | 인증번호 불일치 | 인증번호 재확인 |
| **400** | `AUTHORIZATION_HEADER_MISSING` | Authorization 헤더 누락/형식 오류 | 헤더 확인 |
| **401** | `INVALID_CREDENTIALS` | 로그인 정보 불일치 | 이메일/비밀번호 확인 |
| **401** | `INVALID_REFRESH_TOKEN` | 유효하지 않은 리프레시 토큰 | 재로그인 |
| **403** | `EMAIL_NOT_VERIFIED` | 이메일 인증 미완료 | 인증번호 확인 |
| **404** | `USER_NOT_FOUND` | 사용자를 찾을 수 없음 | 가입 여부 확인 |
| **404** | `VERIFICATION_CODE_NOT_FOUND` | 인증번호 미존재 | 인증번호 재요청 |
| **409** | `EMAIL_ALREADY_REGISTERED` | 이미 가입된 이메일 | 로그인 또는 다른 이메일 사용 |
| **410** | `EXPIRED_VERIFICATION_CODE` | 인증번호 만료 | 인증번호 재요청 |
| **429** | `MAX_VERIFICATION_ATTEMPTS_EXCEEDED` | 인증번호 시도 초과 | 인증번호 재요청 |

자세한 내용은 [에러 처리 규약](../conventions/error-handling.md)을 참고하세요.

---

## 🏗️ 아키텍처 및 서비스 컴포넌트

### 핵심 서비스

#### AuthService
- **역할**: 회원가입, 로그인, 토큰 관리의 핵심 비즈니스 로직
- **위치**: `auth/application/AuthService.java`
- **주요 기능**:
  - 회원가입 처리 및 이메일 인증 검증
  - 로그인 인증 및 JWT 토큰 발급
  - 리프레시 토큰 갱신
  - 비밀번호 암호화 (BCrypt)

#### EmailService
- **역할**: 이메일 인증번호 발송 및 HTML 템플릿 관리
- **위치**: `auth/application/EmailService.java`
- **구현**: JavaMailSender 사용, SMTP 설정 필요
- **주요 기능**:
  - 6자리 랜덤 인증번호 생성
  - HTML 기반 이메일 템플릿 제공
  - 인증번호 유효 시간 관리 (5분)
  - EmailVerificationCode 엔티티 저장

#### EmailVerificationCleanupService
- **역할**: 만료된 이메일 인증 코드 자동 정리
- **위치**: `auth/application/EmailVerificationCleanupService.java`
- **실행 주기**: 매일 자정 실행 (`@Scheduled`)
- **동작**: 만료 시간이 지난 인증 코드를 DB에서 삭제

#### TokenBlacklistService
- **역할**: 로그아웃된 토큰을 블랙리스트로 관리
- **위치**: `auth/application/TokenBlacklistService.java`
- **주요 기능**:
  - 로그아웃 시 AccessToken을 블랙리스트에 추가
  - 인증 필터에서 블랙리스트 토큰 검증
  - 만료된 블랙리스트 항목 자동 정리

#### CustomUserDetailsService
- **역할**: Spring Security 통합을 위한 UserDetailsService 구현
- **위치**: `auth/application/CustomUserDetailsService.java`
- **기능**:
  - 이메일로 사용자 정보 로드
  - User 엔티티 → UserDetails 변환
  - Spring Security 인증 프로세스와 통합

#### CustomUserDetails
- **역할**: Spring Security UserDetails 인터페이스 구현
- **위치**: `auth/application/CustomUserDetails.java`
- **기능**:
  - User 엔티티의 정보를 Security Context에 제공
  - 권한(authorities) 정보 제공
  - 계정 상태 (활성화, 잠금 등) 관리

### 관련 엔티티
- **User**: 사용자 정보 저장 ([User 스키마](../database/schema/user.md))
- **EmailVerificationCode**: 이메일 인증번호 저장 ([EmailVerification 스키마](../database/schema/emailverification.md))
- **Organization**: 조직 정보 ([User 스키마](../database/schema/user.md#organization-테이블))

### 보안 구성
- **JwtTokenProvider**: JWT 토큰 생성 및 검증 (`global/security/jwt/`)
- **JwtAuthenticationFilter**: JWT 기반 인증 필터
- **SecurityConfig**: Spring Security 설정 (`global/security/SecurityConfig.java`)

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [User 테이블 스키마](../database/schema/user.md) - User, Organization 엔티티
- **필수**: [CodeType.RL](../database/schema/code-system.md#rl-역할) - 역할 코드
- **참고**: [에러 처리 규약](../conventions/error-handling.md) - 에러 응답 형식
- **참고**: [API 설계 규약](../conventions/api-conventions.md) - JWT 인증 구조

### 상위/하위 문서
- ⬆️ **상위**: [API 문서 홈](./README.md)
- ➡️ **다음**: [근무 일정 API](./schedule.md)

### 관련 아키텍처
- **Controller**: `auth/controller/AuthController.java`
- **Application**: `auth/application/AuthService.java`, `TokenBlacklistService.java`
- **Domain**: `domain/user/entity/User.java`
- **Security**: `global/security/jwt/JwtTokenProvider.java`

자세한 내용은 [아키텍처 문서](../architecture/overview.md#auth-모듈)를 참고하세요.
