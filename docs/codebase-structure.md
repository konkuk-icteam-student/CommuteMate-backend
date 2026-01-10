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
- **Swagger/OpenAPI 3.0**: API 자동 문서화

---

## Project Structure

```
src/main/java/com/better/CommuteMate/
├── CommuteMateApplication.java       # Spring Boot 진입점
│
├── auth/                              # 인증 모듈
│   ├── application/
│   │   ├── AuthService.java
│   │   ├── CustomUserDetails.java
│   │   ├── CustomUserDetailsService.java
│   │   ├── EmailService.java
│   │   ├── EmailVerificationCleanupService.java
│   │   ├── TokenBlacklistService.java
│   │   └── dto/
│   │       └── AuthTokens.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       ├── RegisterRequest.java
│   │       ├── RegisterResponse.java
│   │       ├── SendVerificationCodeRequest.java
│   │       └── VerifyCodeRequest.java
│
├── schedule/                          # 근무 일정 모듈
│   ├── application/
│   │   ├── AdminScheduleService.java
│   │   ├── MonthlyScheduleConfigService.java
│   │   ├── ScheduleService.java
│   │   ├── ScheduleValidator.java
│   │   ├── dtos/
│   │   │   ├── ApplyScheduleResultCommand.java
│   │   │   ├── MonthlyScheduleConfigCommand.java
│   │   │   ├── SetApplyTermCommand.java
│   │   │   └── WorkScheduleCommand.java
│   │   └── exceptions/
│   │       ├── ScheduleAllFailureException.java
│   │       ├── ScheduleConfigException.java
│   │       ├── ScheduleErrorCode.java
│   │       ├── SchedulePartialFailureException.java
│   │       └── response/
│   │           ├── ApplyTermValidationResponseDetail.java
│   │           └── ScheduleResponseDetail.java
│   ├── controller/
│   │   ├── admin/
│   │   │   ├── AdminScheduleController.java
│   │   │   └── dtos/
│   │   │       ├── ApplyTermResponse.java
│   │   │       ├── MonthlyLimitResponse.java
│   │   │       ├── MonthlyLimitsResponse.java
│   │   │       ├── ProcessChangeRequestRequest.java
│   │   │       ├── SetApplyTermRequest.java
│   │   │       └── SetMonthlyLimitRequest.java
│   │   └── schedule/
│   │       ├── WorkScheduleController.java
│   │       ├── WorkWebsocketController.java
│   │       └── dtos/
│   │           ├── ApplyWorkSchedule.java
│   │           ├── ApplyWorkScheduleResponseDetail.java
│   │           └── ModifyWorkScheduleDTO.java
│   └── ScheduleNotificationAspect.java
│
├── category/                          # FAQ 대분류 모듈
│   ├── application/
│   │   ├── CategoryService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── PostCategoryRegisterRequest.java
│   │       │   ├── PutCategoryUpdateRequest.java
│   │       │   ├── PostManagerSubCategoryRequest.java
│   │       │   └── PutManagerSubCategoryRequest.java
│   │       └── response/
│   │           ├── GetCategoryListResponse.java
│   │           ├── GetCategoryListWrapper.java
│   │           ├── PostCategoryRegisterResponse.java
│   │           ├── PutCategoryUpdateResponse.java
│   │           └── PostManagerSubCategoryResponse.java
│   └── controller/
│       └── CategoryController.java
│
├── subcategory/                       # FAQ 소분류 모듈
│   ├── application/
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── PostSubCategoryRegisterRequest.java
│   │       │   └── PostSubCategoryUpdateNameRequest.java
│   │       └── response/
│   │           ├── DeleteSubCategoryResponse.java
│   │           ├── PatchFavoriteSubCategoryResponse.java
│   │           ├── PostSubCategoryRegisterResponse.java
│   │           └── PostSubCategoryUpdateNameResponse.java
│   └── controller/
│       └── SubCategoryController.java
│
├── manager/                           # 매니저 모듈
│   ├── application/
│   │   └── ManagerService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── PostManagerSubCategoryRequest.java
│   │       │   └── PutManagerSubCategoryRequest.java
│   │       └── response/
│   │           └── PostManagerSubCategoryResponse.java
│   └── controller/
│       └── ManagerController.java
│
├── faq/                               # FAQ 모듈
│   ├── application/
│   │   └── FaqService.java
│   └── controller/
│       └── FaqController.java
│
├── manager/                           # 매니저 관리 모듈
│   ├── application/
│   │   └── ManagerService.java
│   └── controller/
│       └── ManagerController.java
│
├── domain/                            # 도메인 엔티티 및 리포지토리
│   ├── category/
│   │   ├── entity/
│   │   │   ├── Category.java
│   │   │   ├── SubCategory.java
│   │   │   └── ManagerSubCategory.java
│   │   └── repository/
│   │       ├── CategoryRepository.java
│   │       ├── SubCategoryRepository.java
│   │       └── ManagerSubCategoryRepository.java
│   ├── code/
│   │   ├── entity/
│   │   │   ├── Code.java
│   │   │   ├── CodeMajor.java
│   │   │   ├── CodeSub.java
│   │   │   └── CodeSubId.java
│   │   └── repository/
│   │       ├── CodeMajorRepository.java
│   │       ├── CodeRepository.java
│   │       └── CodeSubRepository.java
│   ├── emailverification/
│   │   ├── entity/
│   │   │   └── EmailVerificationCode.java
│   │   └── repository/
│   │       └── EmailVerificationCodeRepository.java
│   ├── faq/
│   │   ├── entity/
│   │   │   ├── Faq.java
│   │   │   └── FaqHistory.java
│   │   ├── repository/
│   │   │   ├── FaqHistoryRepository.java
│   │   │   └── FaqRepository.java
│   │   └── dto/
│   │       └── request/
│   │           ├── FaqCreateRequest.java
│   │           └── FaqUpdateRequest.java
│   ├── organization/
│   │   ├── entity/
│   │   │   └── Organization.java
│   │   └── repository/
│   │       └── OrganizationRepository.java
│   ├── schedule/
│   │   ├── entity/
│   │   │   ├── MonthlyScheduleConfig.java
│   │   │   └── WorkSchedule.java
│   │   └── repository/
│   │       ├── MonthlyScheduleConfigRepository.java
│   │       └── WorkSchedulesRepository.java
│   ├── user/
│   │   ├── entity/
│   │   │   └── User.java
│   │   └── repository/
│   │       └── UserRepository.java
│   ├── workattendance/
│   │   ├── entity/
│   │   │   └── WorkAttendance.java
│   │   └── repository/
│   │       └── WorkAttendanceRepository.java
│   └── workchangerequest/
│       ├── entity/
│       │   └── WorkChangeRequest.java
│       └── repository/
│           └── WorkChangeRequestRepository.java
│
└── global/                            # 전역 설정 및 공통 코드
    ├── code/
    │   ├── CodeType.java              # 코드 타입 Enum (중앙 관리)
    │   └── CodeTypeConverter.java     # JPA 자동 변환 컨버터
    ├── config/
    │   └── WebsocketConfig.java
    ├── controller/
    │   ├── dtos/
    │   │   ├── ErrorResponseDetail.java
    │   │   ├── Response.java
    │   │   └── ResponseDetail.java
    │   └── GlobalExceptionHandler.java  # 전역 예외 핸들러
    ├── exceptions/
    │   ├── error/
    │   │   ├── AuthErrorCode.java
    │   │   ├── CategoryErrorCode.java
    │   │   ├── CustomErrorCode.java
    │   │   ├── GlobalErrorCode.java
    │   │   └── SubcategoryErrorCode.java
    │   ├── response/
    │   │   └── UserNotFoundResponseDetail.java
    │   ├── AuthException.java
    │   ├── BasicException.java         # 모든 예외의 베이스 클래스
    │   ├── CategoryException.java
    │   └── SubCategoryException.java
    └── security/
        ├── jwt/
        │   ├── JwtAuthenticationFilter.java
        │   └── JwtTokenProvider.java
        └── SecurityConfig.java
```

---

## Architecture Pattern

### Layered Architecture (계층형 아키텍처)

```
┌─────────────────────────────────────────┐
│         Controller Layer                 │  ← REST API 엔드포인트, 요청/응답 처리
├─────────────────────────────────────────┤
│      Application Layer                   │  ← 비즈니스 로직, 트랜잭션 관리
├─────────────────────────────────────────┤
│        Domain Layer                      │  ← 엔티티, 리포지토리 (데이터 접근)
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

---

## Key Design Patterns

### 1. DTO Pattern
데이터 전송 객체(DTO)를 사용하여 계층 간 데이터 전송:

```
Controller DTO (Request/Response)
    ↓
Application DTO (Command)
    ↓
Entity
```

### 2. Repository Pattern
Spring Data JPA Repository를 사용한 데이터 접근 추상화:
- `UserRepository extends JpaRepository<User, Integer>`
- Custom Query Methods (`findByEmail`, `findByOrganizationId`)

### 3. Service Pattern
비즈니스 로직을 Service 클래스로 캡슐화:
- `AuthService`, `ScheduleService`, `CategoryService`, etc.

### 4. Exception Handling Pattern
- **Base Exception**: `BasicException` (모든 예외의 부모)
- **Custom Exceptions**: 도메인별 예외 클래스 (예: `ScheduleAllFailureException`)
- **Error Codes**: `ErrorCode` 인터페이스를 구현한 Enum
- **Global Handler**: `GlobalExceptionHandler` (`@RestControllerAdvice`)

### 5. Command Pattern (Schedule Module)
일정 관련 작업에 Command 객체 사용:
- `WorkScheduleCommand`: 일정 신청 명령
- `ApplyScheduleResultCommand`: 일정 신청 결과
- `MonthlyScheduleConfigCommand`: 월별 설정 명령

### 6. Type-Safe Code System
`CodeType` Enum을 사용한 타입 안전한 코드 관리:
- 중앙 집중식 코드 정의 (`global/code/CodeType.java`)
- JPA Attribute Converter를 통한 자동 변환
- String 대신 Enum 사용하여 컴파일 타임 타입 체크

---

## Key Components

### Global Components

#### Response<T>
모든 API 응답의 표준 래퍼 클래스:
```java
public class Response<T> {
    private boolean isSuccess;
    private String message;
    private T details;
}
```

#### BasicException
모든 예외의 베이스 클래스:
```java
public abstract class BasicException extends RuntimeException {
    private final CustomErrorCode errorCode;
    private final ResponseDetail<?> details;
}
```

#### CodeType Enum
시스템 전체에서 사용하는 코드 타입:
```java
public enum CodeType {
    WS01("WS", "01", "REQUESTED", "신청"),
    RL01("RL", "01", "STUDENT", "학생"),
    // ...
}
```

#### GlobalExceptionHandler
전역 예외 핸들러 (`@RestControllerAdvice`):
- 모든 예외를 통일된 형식으로 처리
- HTTP 상태 코드 매핑

---

## Module Overview

### 1. Auth Module (`auth/`)

**Purpose**: 사용자 인증 및 인가

**Key Components**:
- `AuthService`: 회원가입, 로그인, 로그아웃, 토큰 갱신
- `CustomUserDetailsService`: Spring Security UserDetailsService 구현
- `TokenBlacklistService`: 로그아웃된 토큰 관리 (블랙리스트)
- `EmailService`: 이메일 인증 코드 발송
- `EmailVerificationCleanupService`: 만료된 인증 코드 정리

**Key Features**:
- JWT 기반 인증 (AccessToken + RefreshToken)
- HttpOnly 쿠키를 통한 AccessToken 전달
- 이메일 인증 기능
- 토큰 블랙리스트 기반 로그아웃

---

### 2. Schedule Module (`schedule/`)

**Purpose**: 근무 일정 관리

**Key Components**:
- `ScheduleService`: 사용자 일정 신청, 조회, 수정
- `AdminScheduleService`: 관리자 일정 관리, 변경 요청 처리
- `MonthlyScheduleConfigService`: 월별 제한 및 신청 기간 설정
- `ScheduleValidator`: 일정 검증 로직

**Key Features**:
- 일괄 일정 신청 (부분 성공/실패 처리)
- 월별 최대 동시 근무 인원수 제한
- 근무 신청 기간 설정
- 근무 일정 변경 요청 승인/거부
- 근무 신청 조회 (사용자)
- 근무 일정 수정 요청

**Special Handling**:
- HTTP 207 (Multi-Status): 일부 성공, 일부 실패
- HTTP 422 (Unprocessable Entity): 모든 실패

---

### 3. Category Module (`category/`)

**Purpose**: FAQ 대분류 관리

**Key Components**:
- `CategoryService`: 대분류 CRUD

**Key Features**:
- 대분류 등록 (중복 체크)
- 대분류 수정
- 전체 대분류 조회
- 대분류 삭제 (하위 소분류 존재 시 불가)

---

### 4. SubCategory Module (`subcategory/`)

**Purpose**: FAQ 소분류 관리

**Key Components**:
- `SubCategoryService`: 소분류 CRUD

**Key Features**:
- 소분류 등록 (같은 대분류 내 중복 체크)
- 소분류 이름 수정
- 소분류 삭제 (하위 FAQ 존재 시 불가)
- 소분류 즐겨찾기 등록/해제

---

### 5. FAQ Module (`faq/`)

**Purpose**: FAQ 게시글 관리

**Key Components**:
- `FaqController`: FAQ API 엔드포인트
- `FaqService`: FAQ 비즈니스 로직

**Implemented Features**:
- FAQ 작성
- FAQ 수정 (수정 이력 자동 저장)

**Planned Features** (TODO):
- FAQ 삭제 (소프트 삭제)
- FAQ 상세 조회
- FAQ 목록 조회 (필터 옵션)
- FAQ 검색 (키워드 + 날짜 범위)
- FAQ 필터 검색 (카테고리 + 날짜 범위)

---

### 6. Manager Module (`manager/`)

**Purpose**: 매니저 및 매니저-소분류 매핑 관리

**Key Components**:
- `ManagerController`: 매니저 API 엔드포인트
- `ManagerService`: 매니저 비즈니스 로직

**Key Features**:
- 매니저 권한 등록 (사용자 역할 변경)
- 매니저-소분류 매핑 등록/수정/삭제
- 매니저 권한 해제

---

### 7. Domain Layer (`domain/`)

**Purpose**: 엔티티 및 리포지토리 정의

**Entity Rules**:
- 모든 엔티티는 감사 필드 포함 (`created_at`, `updated_at`, etc.)
- Lombok 사용 (`@Entity`, `@Getter`, `@Setter`, `@Builder`)
- `@PrePersist`, `@PreUpdate`를 통한 자동 타임스탬프

**Repository Rules**:
- Spring Data JPA `JpaRepository` 상속
- 커스텀 쿼리 메서드 명명 규칙 준수
- 필요시 `@Query` 애너테이션 사용

---

### 7. Global Layer (`global/`)

**Purpose**: 공통 컴포넌트 및 설정

**Key Components**:
- `CodeType`: 타입 안전한 코드 Enum
- `CodeTypeConverter`: JPA Attribute Converter
- `GlobalExceptionHandler`: 전역 예외 처리
- `JwtTokenProvider`: JWT 토큰 생성/검증
- `JwtAuthenticationFilter`: JWT 인증 필터
- `SecurityConfig`: Spring Security 설정

---

## Code Conventions

### 1. Naming Conventions

#### Packages
- **소문자**, 단수형: `auth`, `schedule`, `category`
- **계층 구조**: `application`, `controller`, `domain`

#### Classes
- **PascalCase**: `AuthService`, `WorkScheduleController`
- **Interface**: PascalCase (명사형): `UserRepository`
- **DTO**: PascalCase (용도에 따라 접미사):
  - `Request`: `LoginRequest`, `RegisterRequest`
  - `Response`: `LoginResponse`, `MonthlyLimitResponse`
  - `Command`: `WorkScheduleCommand`

#### Methods
- **camelCase**, 동사형:
  - Service: `register()`, `login()`, `applyWorkSchedules()`
  - Repository: `findByEmail()`, `findByUserId()`

#### Variables/Fields
- **camelCase**: `userId`, `scheduleId`, `createdAt`

---

### 2. Exception Handling

#### Custom Exception Pattern
```java
public class ScheduleAllFailureException extends BasicException {
    public static ScheduleAllFailureException of(
        ScheduleErrorCode errorCode,
        List<ScheduleFailureDetail> failures
    ) {
        return new ScheduleAllFailureException(errorCode, failures);
    }
}
```

#### Exception Handling Flow
```
Controller
    ↓ (throw exception)
GlobalExceptionHandler
    ↓ (catch & map to HTTP status)
Response<T> (error response)
```

---

### 3. Transaction Management

#### Service Layer
```java
@Service
public class ScheduleService {
    @Transactional
    public ApplyScheduleResultCommand applyWorkSchedules(...) {
        // 비즈니스 로직
        // 실패 시 자동 롤백
    }
}
```

#### Transaction Boundaries
- Service 메서드 레벨에서 트랜잭션 시작
- Repository는 트랜잭션 없이 데이터 접근
- 예외 발생 시 자동 롤백 (`RuntimeException`)

---

### 4. Validation

#### Controller Layer
```java
@PostMapping("/register")
public ResponseEntity<Response> register(
    @Valid @RequestBody RegisterRequest request
) {
    // @Valid로 자동 검증
}
```

#### Application Layer
```java
// Service 내에서 수동 검증
ScheduleValidator.validateSchedule(schedule);
```

---

### 5. Security

#### JWT Token Flow
```
Client Request (RefreshToken in Header)
    ↓
JwtAuthenticationFilter
    ↓
JwtTokenProvider.validateToken()
    ↓
SecurityContext.setAuthentication()
    ↓
Controller → Service → Repository
```

#### Token Management
- **AccessToken**: HttpOnly 쿠키 (자동 전달)
- **RefreshToken**: Authorization 헤더 (`Bearer <token>`)
- **Blacklist**: 로그아웃 시 토큰 블랙리스트 등록

---

## Testing Structure

```
src/test/java/com/better/CommuteMate/
├── application/
│   └── schedule/
│       ├── ScheduleServiceTest.java
│       └── ScheduleValidatorTest.java
├── controller/
│   └── admin/
│       └── AdminScheduleControllerTest.java
└── CommuteMateApplicationTests.java
```

**Testing Conventions**:
- 단위 테스트: Service, Validator 로직 테스트
- 통합 테스트: Controller API 엔드포인트 테스트
- Test 클래스명: `{TargetClass}Test.java`

---

## Configuration Files

### `src/main/resources/application.yaml`
Spring Boot 설정 파일:
- 데이터베이스 연결 설정
- JPA/Hibernate 설정
- JWT 설정
- 로그 레벨 설정

### `build.gradle`
Gradle 빌드 설정:
- 의존성 관리
- 플러그인 설정
- 빌드 태스크 정의

---

## Development Workflow

### 1. New Feature Implementation

1. **Define Entities** (if needed):
   - Create Entity in `domain/{domain}/entity/`
   - Create Repository in `domain/{domain}/repository/`

2. **Create Application Layer**:
   - Create Service in `{module}/application/`
   - Create DTOs in `{module}/application/dtos/`

3. **Create Controller Layer**:
   - Create Controller in `{module}/controller/`
   - Create Request/Response DTOs in `{module}/controller/dtos/`

4. **Add Error Handling** (if needed):
   - Create ErrorCode enum in `global/exceptions/error/`
   - Create Exception class in `global/exceptions/`
   - Add handler in `GlobalExceptionHandler`

5. **Write Tests**:
   - Unit tests for Service/Validator
   - Integration tests for Controller

---

### 2. Code Review Checklist

- [ ] Layer separation maintained (Controller → Application → Domain)
- [ ] DTOs properly defined and used
- [ ] Exception handling implemented
- [ ] Transactions properly annotated
- [ ] Naming conventions followed
- [ ] Audit fields included (created_at, updated_at)
- [ ] CodeType used for code fields
- [ ] Tests written and passing
- [ ] API documentation updated (Swagger annotations)

---

## Common Pitfalls

### 1. Skipping Layer Separation
❌ Controller에서 직접 Repository 호출
✅ Controller → Service → Repository

### 2. Missing Transactions
❌ Service 메서드에 `@Transactional` 없음
✅ 비즈니스 로직 메서드에 `@Transactional` 추가

### 3. Using String for Codes
❌ `String statusCode = "WS01";`
✅ `CodeType statusCode = CodeType.WS01;`

### 4. Throwing Generic Exceptions
❌ `throw new RuntimeException("Error");`
✅ `throw new ScheduleAllFailureException(ScheduleErrorCode.XXX, details);`

### 5. Missing Audit Fields
❌ 엔티티에 `created_at`, `updated_at` 없음
✅ 감사 필드 포함 및 `@PrePersist`, `@PreUpdate` 구현

---

## Quick Navigation Guide

### Finding API Endpoints
- **Controller**: `*Module*/controller/*Controller.java`
- **Request DTOs**: `*Module*/controller/dto/*Request.java`
- **Response DTOs**: `*Module*/controller/dto/*Response.java`

### Finding Business Logic
- **Service**: `*Module*/application/*Service.java`
- **Command DTOs**: `*Module*/application/dtos/*Command.java`
- **Validators**: `*Module*/application/*Validator.java`

### Finding Data Models
- **Entity**: `domain/{domain}/entity/*.java`
- **Repository**: `domain/{domain}/repository/*Repository.java`

### Finding Error Codes
- **Global**: `global/exceptions/error/GlobalErrorCode.java`
- **Domain-specific**: `global/exceptions/error/*ErrorCode.java`
  - `AuthErrorCode`: 인증 관련 에러 코드
  - `CategoryErrorCode`: 카테고리 관련 에러 코드
  - `SubcategoryErrorCode`: 소분류 관련 에러 코드
  - `ScheduleErrorCode`: 스케줄 관련 에러 코드 (`schedule/application/exceptions/`)
  - `ManagerErrorCode`: 매니저 관련 에러 코드

### Finding Code Types
- **CodeType Enum**: `global/code/CodeType.java`

---

## External Documentation

- [API Documentation](./api.md)
- [Database Schema](./db.md)
- [Project README](../README.md)
- [ER Diagram](https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757)

---

## Notes

### TODO Items
- FAQ 모듈 구현 (`faq/` - 현재 컨트롤러만 존재)
- FAQ 수정 이력 자동 저장 로직 구현
- FAQ 검색 기능 (키워드, 날짜 범위, 카테고리 필터)
- WebSocket 알림 기능 완성 (`WorkWebsocketController`)

### Future Improvements
- 캐싱 레이어 도입 (Redis)
- 메시지 큐 도입 (이메일 발송 비동기화)
- API Rate Limiting
- 감사 로그 시스템 구축
- 성능 모니터링 도구 연동
