# CommuteMate Codebase Structure

## Overview
CommuteMate 백엔드 프로젝트의 전체 코드베이스 구조와 아키텍처 패턴을 설명하는 문서입니다. 이 문서는 AI/개발자가 빠르게 코드베이스를 이해하고 탐색하는 데 도움을 줍니다.

---

## Tech Stack

### Backend Framework
- **Spring Boot 3.x**: Java 기반 백엔드 프레임워크
- **Spring Security**: 인증/인가 처리
- **Spring Data JPA**: ORM 및 데이터베이스 접근
- **Jakarta EE**: 최신 EE 스펙 사용

### Database
- **MySQL**: 관계형 데이터베이스
- **Hibernate**: JPA 구현체

### Build Tool
- **Gradle**: 의존성 관리 및 빌드 도구

### Language
- **Java 11+**: 프로그래밍 언어

### Authentication
- **JWT (JSON Web Token)**: 토큰 기반 인증
- **HttpOnly Cookie**: 보안 강화를 위한 쿠키 기반 토큰 전달

### Documentation
- **Swagger/OpenAPI 3.0**: API 자동 문서화 (코드 기반)

---

## Project Structure

```
src/main/java/com/better/CommuteMate/
├── CommuteMateApplication.java       # Spring Boot 진입점
│
├── auth/                              # 인증 모듈
│   ├── application/                   # AuthService, TokenProvider 등
│   └── controller/                    # AuthController (Login, Register)
│
├── schedule/                          # 근무 일정 모듈
│   ├── application/                   # ScheduleService, Validator
│   └── controller/
│       ├── schedule/                  # WorkScheduleController
│       └── admin/                     # AdminScheduleController
│
├── attendance/                        # 근태/출석 모듈 (QR)
│   ├── application/                   # AttendanceService, QrTokenService
│   └── controller/                    # WorkAttendanceController
│
├── user/                              # 사용자 모듈
│   ├── application/                   # UserService
│   └── controller/                    # UserController
│
├── home/                              # 홈/대시보드 모듈
│   ├── application/                   # HomeService
│   └── controller/                    # HomeController
│
├── category/                          # FAQ 분류 모듈
│   ├── application/                   # CategoryService
│   └── controller/                    # CategoryController
│
├── manager/                           # 매니저 관리 모듈
│   ├── application/                   # ManagerService
│   └── controller/                    # ManagerController
│
├── faq/                               # FAQ 모듈
│   ├── application/                   # FaqService
│   └── controller/                    # FaqController
│
├── task/                              # 업무 관리 모듈
│   ├── application/                   # TaskService, TaskTemplateService
│   └── controller/                    # TaskController, TaskTemplateController
│
├── domain/                            # 도메인 엔티티 및 리포지토리 (Persistence Layer)
│   ├── user/
│   ├── organization/
│   ├── schedule/
│   ├── workattendance/
│   ├── workchangerequest/
│   ├── faq/
│   ├── category/
│   ├── code/
│   ├── emailverification/
│   └── task/
│
└── global/                            # 전역 설정 및 공통 코드
    ├── code/                          # CodeType Enum
    ├── config/                        # SecurityConfig, WebConfig
    ├── controller/                    # GlobalExceptionHandler, Common DTOs
    ├── exceptions/                    # BasicException, CustomErrorCodes
    └── security/                      # JwtAuthenticationFilter
```

---

## Architecture Pattern

### Layered Architecture (계층형 아키텍처)

프로젝트는 엄격한 계층형 아키텍처를 따릅니다.

```
┌─────────────────────────────────────────┐
│         Controller Layer                 │  ← REST API 엔드포인트, 요청/응답 처리
├─────────────────────────────────────────┤
│      Application Layer                   │  ← 비즈니스 로직, 트랜잭션 관리, 도메인 조합
├─────────────────────────────────────────┤
│        Domain Layer                      │  ← 엔티티, 리포지토리 (순수 데이터 접근)
├─────────────────────────────────────────┤
│      Database (MySQL)                   │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. Controller Layer
- **위치**: `*Module*/controller/`
- **책임**:
  - HTTP 요청 수신 및 응답 반환
  - 요청 데이터 검증 (`@Valid`)
  - Application Layer로 위임
  - 응답 형식 표준화 (`Response<T>`)

#### 2. Application Layer
- **위치**: `*Module*/application/`
- **책임**:
  - 비즈니스 로직 실행
  - 트랜잭션 관리 (`@Transactional`)
  - DTO 변환 (Request → Command, Entity → Response)
  - 도메인 로직 조합
  - 예외 처리 및 변환

#### 3. Domain Layer
- **위치**: `domain/`
- **책임**:
  - 엔티티 정의 (데이터 모델)
  - 리포지토리 인터페이스 (데이터 접근 추상화)
  - 도메인 비즈니스 로직 (엔티티 메서드)
  - **참고**: 비즈니스 로직이 복잡해질 경우 도메인 서비스가 추가될 수 있습니다.

---

## Key Design Patterns

### 1. DTO Pattern
데이터 전송 객체(DTO)를 사용하여 계층 간 데이터 전송을 분리합니다. 엔티티를 직접 컨트롤러에서 반환하지 않습니다.

### 2. Repository Pattern
Spring Data JPA Repository를 사용하여 데이터 접근을 추상화합니다.

### 3. Service Pattern
비즈니스 로직을 Service 클래스로 캡슐화하여 재사용성을 높이고 트랜잭션 경계를 설정합니다.

### 4. Exception Handling Pattern
- **Base Exception**: `BasicException`
- **Custom Error Codes**: 도메인별 `ErrorCode` Enum 구현
- **Global Handler**: `GlobalExceptionHandler`에서 일괄 처리하여 표준 JSON 응답 반환

### 5. Type-Safe Code System
`CodeType` Enum을 사용하여 시스템 내의 모든 상태 코드(DB의 char/varchar)를 타입 안전하게 관리합니다. JPA Converter가 자동으로 변환을 처리합니다.

---

## Module Overview

### 1. Auth Module (`auth/`)
사용자 인증, 회원가입, 로그아웃, 토큰 재발급, 이메일 인증을 담당합니다.

### 2. Schedule Module (`schedule/`)
근무 일정 신청, 조회, 수정 및 관리자 설정(월별 제한, 신청 기간)을 담당합니다.

### 3. Attendance Module (`attendance/`)
QR 코드를 이용한 출근/퇴근 체크 및 출석 기록 조회를 담당합니다. 
- **QR Token**: 관리자 태블릿용 일회성 QR 토큰 생성

### 4. User Module (`user/`)
사용자 프로필 조회 및 개인별 근무 시간 통계(주간/월간)를 제공합니다.

### 5. Home Module (`home/`)
메인 대시보드 데이터를 제공합니다.
- 오늘의 근무 시간 요약
- 현재 출근 상태 확인

### 6. FAQ & Category System (`faq/`, `category/`)
FAQ 게시판과 분류를 관리합니다.
- FAQ는 하나의 분류에 속합니다. 
- 분류 단위 즐겨찾기 기능 지원

### 7. Manager Module (`manager/`)
매니저 권한 부여 및 매니저별 담당 분류 매핑을 관리합니다.

---

## Configuration Files

### `src/main/resources/application.yaml`
- 데이터베이스 연결, JPA 설정, JWT 비밀키, 로그 레벨 등이 정의되어 있습니다.

### `build.gradle`
- 프로젝트 의존성 및 플러그인 설정이 되어 있습니다.

---

## External Documentation
- [API Documentation](./api.md)
- [Database Schema](./db.md)
- [Error Conventions](./error-convention.md)
