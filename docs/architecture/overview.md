# 아키텍처 개요 (Architecture Overview)

## 📑 목차
- [개요](#-개요)
- [계층형 아키텍처](#-계층형-아키텍처)
- [모듈 구조](#-모듈-구조)
- [요청 처리 흐름](#-요청-처리-흐름)
- [보안 아키텍처](#-보안-아키텍처)
- [데이터베이스 연동](#-데이터베이스-연동)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate는 **계층형 아키텍처(Layered Architecture)**를 기반으로 설계된 Spring Boot 애플리케이션입니다.
각 계층은 명확한 책임을 가지며, 상위 계층은 하위 계층에만 의존하는 단방향 의존성 구조를 따릅니다.

### 핵심 설계 목표

1. **유지보수성**: 계층 분리를 통한 변경 영향 최소화
2. **확장성**: 모듈화를 통한 기능 추가 용이
3. **테스트 용이성**: 각 계층의 독립적인 테스트 가능
4. **명확성**: 각 계층의 책임이 명확하여 코드 이해 쉬움

---

## 🏗️ 계층형 아키텍처

### 전체 계층 구조

```
┌─────────────────────────────────────┐
│      Presentation Layer             │  ← Controller (HTTP 요청/응답)
│         (Controller)                │
└─────────────────────────────────────┘
              ↓ 의존
┌─────────────────────────────────────┐
│       Application Layer              │  ← Service (비즈니스 로직)
│         (Service)                    │
└─────────────────────────────────────┘
              ↓ 의존
┌─────────────────────────────────────┐
│        Domain Layer                  │  ← Entity, Repository (도메인 모델)
│   (Entity, Repository)               │
└─────────────────────────────────────┘
              ↓ 의존
┌─────────────────────────────────────┐
│     Infrastructure Layer             │  ← Database, External Systems
│   (Database, External APIs)          │
└─────────────────────────────────────┘
```

### 각 계층의 책임

#### 1. Presentation Layer (Controller)

**위치**: `*/controller/`

**책임**:
- HTTP 요청 수신 및 파라미터 검증
- DTO 변환 (Request → Command, Response)
- 적절한 HTTP 상태 코드 반환
- 인증/인가 검증 (Security 통합)

**예시**:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Response<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. Request 검증
        // 2. Service 호출
        AuthTokens tokens = authService.login(request.getEmail(), request.getPassword());
        // 3. Response 변환
        return Response.success("로그인 성공", LoginResponse.from(tokens));
    }
}
```

#### 2. Application Layer (Service)

**위치**: `*/application/`

**책임**:
- 비즈니스 로직 구현
- 트랜잭션 관리 (`@Transactional`)
- 도메인 객체 조작 및 조합
- 예외 처리 및 검증

**예시**:
```java
@Service
@Transactional(readOnly = true)
public class ScheduleService {
    private final WorkSchedulesRepository scheduleRepository;
    private final ScheduleValidator validator;

    @Transactional
    public ApplyScheduleResultCommand applySchedules(List<WorkScheduleCommand> commands) {
        // 1. 검증
        validator.validateSchedules(commands);
        // 2. 비즈니스 로직 실행
        List<WorkSchedule> schedules = commands.stream()
            .map(this::createSchedule)
            .collect(Collectors.toList());
        // 3. 저장
        scheduleRepository.saveAll(schedules);
        // 4. 결과 반환
        return ApplyScheduleResultCommand.success(schedules);
    }
}
```

#### 3. Domain Layer (Entity, Repository)

**위치**: `domain/*/entity/`, `domain/*/repository/`

**책임**:
- 도메인 엔티티 정의 (JPA Entity)
- 데이터 접근 인터페이스 정의 (Repository)
- 도메인 로직 캡슐화 (Entity 내부 메서드)

**예시**:
```java
@Entity
@Table(name = "work_schedule")
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;

    // 도메인 로직: 일정 승인
    public void approve() {
        this.statusCode = CodeType.WS02;  // APPROVED
    }

    // 도메인 로직: 일정 거부
    public void reject() {
        this.statusCode = CodeType.WS03;  // REJECTED
    }
}
```

#### 4. Infrastructure Layer

**위치**: `global/config/`, `global/security/`

**책임**:
- 데이터베이스 연동 (JPA, Hibernate)
- 외부 시스템 통합
- 보안 설정 (Spring Security, JWT)
- 기술적 횡단 관심사 처리

---

## 🧩 모듈 구조

### 모듈 조직 원칙

CommuteMate는 **도메인 중심 모듈화(Domain-Driven Modularity)**를 따릅니다.
각 모듈은 특정 비즈니스 도메인의 모든 계층을 포함합니다.

```
src/main/java/com/better/CommuteMate/
├── auth/                    # 인증 모듈
│   ├── controller/          # Presentation Layer
│   └── application/         # Application Layer
├── schedule/                # 근무 일정 모듈
│   ├── controller/          # Presentation Layer
│   └── application/         # Application Layer
├── domain/                  # 도메인 엔티티 및 리포지토리
│   ├── user/                # Domain Layer (User)
│   ├── schedule/            # Domain Layer (Schedule)
│   └── ...
└── global/                  # 전역 설정 및 공통 코드
    ├── config/              # Infrastructure
    ├── security/            # Infrastructure
    └── exceptions/          # 예외 처리
```

### 주요 모듈 설명

#### 1. auth/ - 인증 모듈

**책임**: 사용자 인증 및 권한 관리

**구성요소**:
- `controller/AuthController`: 회원가입, 로그인, 로그아웃 API
- `application/AuthService`: 인증 비즈니스 로직
- `application/TokenBlacklistService`: 토큰 블랙리스트 관리
- `application/CustomUserDetailsService`: Spring Security 통합

**주요 기능**:
- JWT 기반 인증 (AccessToken + RefreshToken)
- 토큰 블랙리스트를 통한 로그아웃
- Spring Security와 통합된 인증/인가

#### 2. schedule/ - 근무 일정 모듈

**책임**: 근무 일정 관리 (신청, 조회, 수정)

**구성요소**:
- `controller/schedule/WorkScheduleController`: 사용자 일정 API
- `controller/admin/AdminScheduleController`: 관리자 일정 API
- `application/ScheduleService`: 일정 비즈니스 로직
- `application/MonthlyScheduleConfigService`: 월별 제한/신청 기간 관리
- `application/ScheduleValidator`: 일정 검증 로직

**주요 기능**:
- 근무 일정 신청/조회/수정 (사용자)
- 월별 일정 제한 관리 (관리자)
- 부분 성공/실패 처리 (HTTP 207, 422)

#### 3. domain/ - 도메인 계층

**책임**: 엔티티 정의 및 데이터 접근

**구성요소**:
- `domain/user/`: User 엔티티 및 UserRepository
- `domain/schedule/`: WorkSchedule, MonthlyScheduleConfig 엔티티
- `domain/code/`: 코드 마스터 엔티티 (Code, CodeMajor, CodeSub)
- `domain/faq/`: FAQ 시스템 엔티티

**특징**:
- JPA Entity로 데이터베이스 테이블 매핑
- Spring Data JPA Repository로 데이터 접근
- CodeType Enum을 사용한 타입 안전성

#### 4. global/ - 전역 설정

**책임**: 애플리케이션 전역 설정 및 공통 코드

**구성요소**:
- `global/config/`: Spring 설정 클래스
- `global/security/`: JWT 인증 필터, SecurityConfig
- `global/exceptions/`: 전역 예외 처리
- `global/code/`: CodeType Enum 시스템
- `global/controller/`: GlobalExceptionHandler

**특징**:
- 모든 모듈에서 공통으로 사용하는 설정 및 유틸리티
- 횡단 관심사(Cross-Cutting Concerns) 처리

---

## 🔄 요청 처리 흐름

### 1. 일반 API 요청 흐름

```
Client
  ↓ HTTP Request
[JwtAuthenticationFilter] ← JWT 토큰 검증
  ↓
[Controller] ← 요청 수신, DTO 검증
  ↓
[Service] ← 비즈니스 로직 실행
  ↓
[Repository] ← 데이터 접근
  ↓
[Database] ← SQL 실행
  ↓
[Repository] ← Entity 반환
  ↓
[Service] ← 결과 처리
  ↓
[Controller] ← Response 변환
  ↓
[GlobalExceptionHandler] ← 예외 처리 (필요 시)
  ↓ HTTP Response
Client
```

### 2. 인증 요청 흐름 (로그인)

```
Client
  ↓ POST /api/auth/login
[AuthController]
  ↓ LoginRequest (email, password)
[AuthService]
  ↓ 이메일, 비밀번호
[UserRepository] → User 조회
  ↓ User Entity
[PasswordEncoder] → 비밀번호 검증
  ↓ 검증 성공
[JwtTokenProvider] → AccessToken + RefreshToken 생성
  ↓ AuthTokens (accessToken, refreshToken, expiresIn)
[AuthService] → User.refreshToken 업데이트 (DB 저장)
  ↓ User Entity (refreshToken 포함)
[AuthService] → LoginResponse 변환
  ↓ LoginResponse (accessToken, refreshToken, expiresIn)
[AuthController] → Response<LoginResponse>
  ↓ HTTP 200 OK (Body + Cookie)
Client (토큰을 로컬스토리지/메모리/쿠키에 저장)
```

### 3. 근무 일정 신청 흐름

```
Client (인증된 사용자)
  ↓ POST /api/work-schedules/apply (with JWT)
[JwtAuthenticationFilter] → 토큰 검증, 사용자 인증
  ↓ Authentication
[WorkScheduleController]
  ↓ ApplyWorkSchedule[] (DTO)
[ScheduleService]
  ↓ WorkScheduleCommand[] (Command)
[ScheduleValidator] → 일정 검증 (날짜, 시간, 월별 제한)
  ↓ 검증 성공
[ScheduleService] → WorkSchedule 엔티티 생성
  ↓ List<WorkSchedule>
[WorkSchedulesRepository] → DB 저장
  ↓ 저장된 Entity
[ScheduleService] → ApplyScheduleResultCommand 생성
  ↓ ApplyScheduleResultCommand
[WorkScheduleController] → Response 변환
  ↓ Response<ApplyScheduleResultCommand>
Client
  - 201 Created: 모든 일정 신청 성공
  - 207 Multi-Status: 일부 성공, 일부 실패
  - 422 Unprocessable Entity: 모든 일정 신청 실패
```

---

## 🔒 보안 아키텍처

### Spring Security Filter Chain

```
HTTP Request
  ↓
[SecurityContextPersistenceFilter]
  ↓
[JwtAuthenticationFilter] ← Custom JWT 필터
  ↓ JWT 토큰 검증
  ↓ SecurityContext 설정
[AuthorizationFilter] ← 권한 검증
  ↓
[Controller] ← 인증된 요청 처리
  ↓
HTTP Response
```

### JWT 인증 흐름

#### 토큰 구조

**AccessToken**:
- 유효 기간: 1시간
- 전달 방식: 응답 본문 + Authorization 헤더 (Bearer {token}) + HttpOnly Cookie (accessToken)
- 용도: API 요청 인증

**RefreshToken**:
- 유효 기간: 7일
- 저장 위치: 데이터베이스 (User.refreshToken)
- 전달 방식: 응답 본문
- 용도: AccessToken 재발급

#### 인증 프로세스

1. **로그인**:
   ```
   Client → POST /api/auth/login
          → AuthService.login()
          → JwtTokenProvider.generateTokens()
          → AccessToken + RefreshToken 반환 (Body + Cookie)
   ```

2. **API 요청**:
   ```
   Client → GET /api/work-schedules
             Authorization: Bearer {AccessToken}
          → JwtAuthenticationFilter.doFilterInternal()
          → Authorization 헤더에서 토큰 추출
          → JwtTokenProvider.validateToken()
          → SecurityContext에 Authentication 설정
          → Controller 실행
   ```

3. **로그아웃**:
   ```
   Client → POST /api/auth/logout
             Authorization: Bearer {AccessToken}
          → TokenBlacklistService.blacklist()
          → AccessToken 블랙리스트 추가
          → User.refreshToken = null (DB 업데이트)
   ```

### 권한 관리

**CodeType.RL (Role Code)**:
- `RL01`: STUDENT (일반 사용자)
- `RL02`: ADMIN (관리자)

**Spring Security Authority 형식**:
- STUDENT → Authority: `ROLE_RL01`
- ADMIN → Authority: `ROLE_RL02`

**권한별 접근 제어 예시**:
```java
// 관리자만 접근 가능
@PreAuthorize("hasRole('RL02')")  // ROLE_RL02 자동 매핑
public Response<MonthlyLimitResponse> setMonthlyLimit(...) { ... }

// 또는 SimpleGrantedAuthority 직접 확인
if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RL02"))) {
    // 관리자 작업 수행
}
```

**인증 요구 API**:
- 기본: 모든 API는 권한 없이 접근 가능 (permitAll)
- **예외**: `/api/tasks/**`, `/api/task-templates/**` → 인증 필수

---

## 💾 데이터베이스 연동

### JPA/Hibernate 아키텍처

```
Application Layer (Service)
  ↓ Repository 인터페이스 호출
Domain Layer (Repository)
  ↓ Spring Data JPA
JPA Provider (Hibernate)
  ↓ SQL 생성 및 실행
Database (H2/PostgreSQL)
```

### Entity ↔ Database 매핑

**Entity 정의**:
```java
@Entity
@Table(name = "work_schedule")
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer scheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

**Repository 인터페이스**:
```java
public interface WorkSchedulesRepository extends JpaRepository<WorkSchedule, Integer> {
    List<WorkSchedule> findValidSchedulesByUserAndDateRange(Integer userId,
                                                            LocalDateTime start,
                                                            LocalDateTime end);

    Optional<WorkSchedule> findByUserAndStartTime(User user, LocalDateTime startTime);
}
```

### 트랜잭션 관리

**서비스 계층에서 트랜잭션 관리**:
```java
@Service
@Transactional(readOnly = true)  // 기본값: 읽기 전용
public class ScheduleService {

    @Transactional  // 쓰기 작업: 읽기/쓰기 트랜잭션
    public ApplyScheduleResultCommand applySchedules(List<WorkScheduleCommand> commands) {
        // 트랜잭션 내에서 실행
        // 예외 발생 시 자동 롤백
    }
}
```

### CodeType Enum 저장

**@Enumerated를 통한 자동 저장**:
```java
@Entity
@Table(name = "work_schedule")
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @Column(name = "status_code", nullable = false)
    @Enumerated(EnumType.STRING)  // "WS01" 문자열로 저장
    private CodeType statusCode;
}
```

**CodeType Enum**:
```java
public enum CodeType {
    // 근무 상태 (WS)
    WS01("WS", "01", "신청됨"),
    WS02("WS", "02", "승인됨"),
    WS03("WS", "03", "거부됨"),

    // 역할 (RL)
    RL01("RL", "01", "학생"),
    RL02("RL", "02", "관리자");

    private final String major;
    private final String sub;
    private final String description;

    // @Enumerated(EnumType.STRING)으로 저장되므로
    // DB에는 "WS01", "RL01" 등 Enum 이름으로 저장됨
}
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [코드베이스 구조](./codebase-structure.md) - 물리적 파일 조직 및 패키지 구조
- **필수**: [설계 결정 기록](./design-decisions.md) - 아키텍처 선택 이유 및 근거
- **필수**: [API 문서](../api/README.md) - API 엔드포인트 상세 정보
- **필수**: [데이터베이스 문서](../database/README.md) - 데이터베이스 스키마 및 ERD
- **참고**: [개발 규약](../conventions/README.md) - 코딩 스타일 및 설계 규약

### 상위/하위 문서
- ⬆️ **상위**: [아키텍처 README](./README.md)
- ➡️ **관련**:
  - [아키텍처 개요](./overview.md)
  - [코드베이스 구조](./codebase-structure.md)

### 실무 적용
- **신규 개발자**: 이 문서를 먼저 읽어 전체 구조 이해
- **기능 개발**: 계층별 책임 확인 후 적절한 위치에 코드 작성
- **아키텍처 변경**: 설계 원칙을 준수하며 변경 사항 적용
