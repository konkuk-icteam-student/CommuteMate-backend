# 코딩 스타일 가이드

## 📑 목차
- [개요](#-개요)
- [네이밍 컨벤션](#-네이밍-컨벤션)
- [패키지 구조](#-패키지-구조)
- [클래스 작성 규칙](#-클래스-작성-규칙)
- [메서드 작성 규칙](#-메서드-작성-규칙)
- [주석 작성 가이드](#-주석-작성-가이드)
- [포맷팅 규칙](#-포맷팅-규칙)
- [Git 컨벤션](#-git-컨벤션)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 프로젝트의 Java/Spring Boot 코딩 스타일 표준을 정의합니다.
일관된 코드 품질과 가독성을 유지하기 위한 가이드라인입니다.

### 핵심 원칙
- **가독성 우선**: 코드는 사람이 읽기 위해 작성됨
- **일관성**: 프로젝트 전체에서 통일된 스타일
- **명확성**: 의도가 분명한 네이밍과 구조
- **단순성**: 불필요한 복잡도 제거

---

## 🏷️ 네이밍 컨벤션

### 클래스 네이밍

**원칙**: PascalCase 사용

| 타입 | 규칙 | 예시 |
|------|------|------|
| **Entity** | 명사, 도메인 개념 | `User`, `WorkSchedule`, `Organization` |
| **Controller** | `{리소스}Controller` | `AuthController`, `WorkScheduleController` |
| **Service** | `{리소스}Service` | `AuthService`, `ScheduleService` |
| **Repository** | `{엔티티}Repository` | `UserRepository`, `WorkScheduleRepository` |
| **DTO** | `{목적}{리소스}` | `RegisterRequest`, `LoginResponse` |
| **Exception** | `{상황}Exception` | `UserNotFoundException`, `ScheduleAllFailureException` |
| **Enum** | 명사 | `CodeType`, `GlobalErrorCode`, `ScheduleErrorCode` |

**예시**:
```java
// ✅ 올바른 클래스 네이밍
public class User { }
public class AuthController { }
public class ScheduleService { }
public class UserRepository { }
public class RegisterRequest { }
public class UserNotFoundException extends BasicException { }
public enum CodeType { }

// ❌ 잘못된 클래스 네이밍
public class user { }  // 소문자 시작
public class authController { }  // camelCase
public class ScheduleServiceImpl { }  // Impl 불필요 (인터페이스 없으면)
public class scheduleRepo { }  // 축약형 사용
```

### 메서드 네이밍

**원칙**: camelCase 사용, 동사로 시작

| 목적 | 패턴 | 예시 |
|------|------|------|
| **조회 (단건)** | `get{Entity}`, `find{Entity}` | `getUser()`, `findById()` |
| **조회 (목록)** | `get{Entity}List`, `findAll()` | `getUserList()`, `findAll()` |
| **생성** | `create{Entity}`, `save{Entity}` | `createUser()`, `saveSchedule()` |
| **수정** | `update{Entity}`, `modify{Entity}` | `updateUser()`, `modifySchedule()` |
| **삭제** | `delete{Entity}`, `remove{Entity}` | `deleteUser()`, `removeSchedule()` |
| **검증** | `validate{Condition}`, `is{Condition}` | `validateEmail()`, `isValidToken()` |
| **변환** | `to{Type}`, `{Type}From` | `toDTO()`, `entityFromCommand()` |

**예시**:
```java
// ✅ 올바른 메서드 네이밍
public User findById(Long userId) { }
public List<WorkSchedule> findAllByUserId(Long userId) { }
public User createUser(RegisterRequest request) { }
public void updateSchedule(Long scheduleId, UpdateRequest request) { }
public boolean isValidEmail(String email) { }
public UserDTO toDTO(User user) { }

// ❌ 잘못된 메서드 네이밍
public User FindById(Long userId) { }  // PascalCase 사용
public User get_user(Long id) { }  // snake_case 사용
public User user() { }  // 동사 없음
public List<User> users() { }  // 동사 없음
```

### 변수 네이밍

**원칙**: camelCase 사용, 의미 있는 이름

| 타입 | 규칙 | 예시 |
|------|------|------|
| **일반 변수** | 명사, camelCase | `userId`, `scheduleDate`, `maxConcurrent` |
| **boolean** | `is`, `has`, `can` 접두사 | `isActive`, `hasPermission`, `canModify` |
| **상수** | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`, `TOKEN_EXPIRY_SECONDS` |
| **Collection** | 복수형 또는 `List` 접미사 | `users`, `schedules`, `userList` |

**예시**:
```java
// ✅ 올바른 변수 네이밍
private Long userId;
private String email;
private LocalDate scheduleDate;
private boolean isActive;
private boolean hasPermission;
private List<User> users;
private static final int MAX_LOGIN_ATTEMPTS = 5;

// ❌ 잘못된 변수 네이밍
private Long UserId;  // PascalCase
private String e;  // 너무 짧음
private LocalDate date;  // 너무 일반적
private boolean active;  // is 접두사 없음
private List<User> userList;  // 불필요한 List 접미사 (이미 복수형)
private static final int maxLoginAttempts = 5;  // 상수는 대문자
```

### 패키지 네이밍

**원칙**: 소문자, 단수형, 도메인/기능 중심

```
com.better.CommuteMate
├── auth              # 인증 모듈
├── schedule          # 근무 일정 모듈
├── domain            # 도메인 엔티티
│   ├── user
│   ├── schedule
│   └── code
└── global            # 전역 설정
    ├── config
    ├── security
    └── exceptions
```

---

## 📂 패키지 구조

### 계층형 아키텍처

```
{module}/
├── controller/       # HTTP 엔드포인트
│   ├── {리소스}Controller.java
│   └── dto/         # Request/Response DTO
├── application/      # 비즈니스 로직
│   ├── {리소스}Service.java
│   ├── dto/         # Command, Internal DTO
│   └── exceptions/  # 도메인별 예외
└── (도메인 공유 시 domain/ 참조)
```

### 도메인 패키지

```
domain/
├── {entity}/
│   ├── entity/      # JPA 엔티티
│   │   └── {Entity}.java
│   └── repository/  # JPA 리포지토리
│       └── {Entity}Repository.java
```

### 전역 패키지

```
global/
├── config/          # Spring 설정
├── security/        # 인증/인가
├── exceptions/      # 전역 예외 처리
├── controller/      # 전역 컨트롤러 (ExceptionHandler)
└── code/            # CodeType Enum
```

---

## 📝 클래스 작성 규칙

### 클래스 구조 순서

```java
public class UserService {
    // 1. 상수
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // 2. 필드
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 3. 생성자
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 4. public 메서드
    public User createUser(RegisterRequest request) {
        // ...
    }

    public User findById(Long userId) {
        // ...
    }

    // 5. private 메서드
    private void validateEmail(String email) {
        // ...
    }

    private boolean isEmailDuplicate(String email) {
        // ...
    }
}
```

### 의존성 주입

**원칙**: 생성자 주입 사용 (필드 주입 금지)

✅ **올바른 방식 (생성자 주입)**:
```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

❌ **잘못된 방식 (필드 주입)**:
```java
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;  // 필드 주입 금지

    @Autowired
    private PasswordEncoder passwordEncoder;
}
```

### Lombok 사용

**권장 어노테이션**:
- `@Getter`, `@Setter`: 필요한 경우만
- `@Builder`: DTO 생성 시
- `@NoArgsConstructor`, `@AllArgsConstructor`: JPA 엔티티

**지양 어노테이션**:
- `@Data`: 너무 많은 기능 포함
- `@ToString`: 순환 참조 위험
- `@EqualsAndHashCode`: 잘못된 비교 로직

```java
// ✅ 적절한 Lombok 사용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String email;
    private String password;

    @Builder
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}

// ❌ 과도한 Lombok 사용
@Entity
@Data  // 너무 많은 기능
@ToString  // 순환 참조 위험
public class User {
    // ...
}
```

---

## 🔧 메서드 작성 규칙

### 메서드 길이

**원칙**: 한 메서드는 한 가지 일만, 최대 20-30줄 이내

✅ **올바른 예시**:
```java
public User createUser(RegisterRequest request) {
    validateEmail(request.getEmail());
    validatePassword(request.getPassword());

    User user = User.builder()
        .email(request.getEmail())
        .password(encodePassword(request.getPassword()))
        .roleCode(request.getRoleCode())
        .build();

    return userRepository.save(user);
}

private void validateEmail(String email) {
    if (userRepository.existsByEmail(email)) {
        throw new EmailAlreadyRegisteredException(email);
    }
}

private String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
}
```

❌ **잘못된 예시 (너무 긴 메서드)**:
```java
public User createUser(RegisterRequest request) {
    // 이메일 검증
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new EmailAlreadyRegisteredException(request.getEmail());
    }

    // 비밀번호 검증
    if (request.getPassword().length() < 8) {
        throw new InvalidPasswordException("비밀번호는 8자 이상이어야 합니다.");
    }

    // 사용자 생성
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRoleCode(request.getRoleCode());

    // 저장
    User savedUser = userRepository.save(user);

    // 환영 이메일 발송
    emailService.sendWelcomeEmail(savedUser.getEmail());

    return savedUser;
}
```

### 메서드 파라미터

**원칙**: 최대 3-4개 이내, 많으면 DTO로 묶기

✅ **올바른 예시**:
```java
// 파라미터가 적을 때
public User findById(Long userId) { }

// 파라미터가 많을 때 → DTO로 묶기
public WorkSchedule createSchedule(WorkScheduleCommand command) {
    // command에 scheduleDate, startTime, endTime 등 포함
}
```

❌ **잘못된 예시**:
```java
public WorkSchedule createSchedule(
    Long userId,
    LocalDate scheduleDate,
    LocalTime startTime,
    LocalTime endTime,
    CodeType statusCode,
    String memo
) {
    // 파라미터가 너무 많음 → DTO로 묶어야 함
}
```

---

## 💬 주석 작성 가이드

### JavaDoc 주석

**대상**: public 클래스, public 메서드, 복잡한 로직

**형식**:
```java
/**
 * 사용자를 생성하고 저장합니다.
 *
 * @param request 회원가입 요청 정보 (이메일, 비밀번호, 역할)
 * @return 생성된 사용자 엔티티
 * @throws EmailAlreadyRegisteredException 이미 가입된 이메일인 경우
 */
public User createUser(RegisterRequest request) {
    // ...
}
```

### 일반 주석

**원칙**: 코드가 설명하지 못하는 **왜(Why)**를 설명

✅ **좋은 주석**:
```java
// JWT 토큰 만료 시간을 1시간으로 설정 (보안 정책 요구사항)
private static final long TOKEN_EXPIRY_SECONDS = 3600;

// 월별 최대 근무 시간 초과 여부 검증
// 비즈니스 규칙: 한 달에 최대 160시간까지만 근무 가능
if (totalWorkTime > MAX_MONTHLY_WORK_TIME) {
    throw new TotalWorkTimeExceededException(totalWorkTime);
}
```

❌ **나쁜 주석 (코드만 반복)**:
```java
// 사용자 ID를 저장
private Long userId;

// 이메일을 저장
private String email;

// 사용자를 생성
User user = new User();
```

---

## 🎨 포맷팅 규칙

### 들여쓰기

**원칙**: 스페이스 4칸

```java
public class UserService {
    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
```

### 중괄호

**원칙**: K&R 스타일 (여는 중괄호는 같은 줄)

```java
// ✅ 올바른 스타일
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}

// ❌ 잘못된 스타일
if (condition)
{
    doSomething();
}
```

### 라인 길이

**원칙**: 최대 120자

```java
// ✅ 긴 줄은 적절히 나누기
User user = User.builder()
    .email(request.getEmail())
    .password(encodePassword(request.getPassword()))
    .roleCode(request.getRoleCode())
    .build();

// ❌ 한 줄이 너무 김
User user = User.builder().email(request.getEmail()).password(encodePassword(request.getPassword())).roleCode(request.getRoleCode()).build();
```

### import 순서

**순서**:
1. Java 표준 라이브러리
2. Spring Framework
3. 외부 라이브러리
4. 프로젝트 내부 패키지

```java
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
```

---

## 📝 Git 컨벤션

### 커밋 메시지 형식

**Conventional Commits 스타일 사용**:

```
<type>(<scope>): <subject>

<body> (선택사항)
```

### Type 분류

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat(auth): 로그인 API 추가` |
| `fix` | 버그 수정 | `fix(schedule): 일정 검증 로직 수정` |
| `docs` | 문서 수정 | `docs(readme): 설치 가이드 업데이트` |
| `refactor` | 코드 리팩토링 | `refactor(auth): 인증 서비스 구조 개선` |
| `test` | 테스트 추가/수정 | `test(schedule): 일정 서비스 테스트 추가` |
| `chore` | 빌드 설정, 기타 | `chore(deps): Spring Boot 3.2 업그레이드` |
| `style` | 코드 포맷팅 | `style(auth): 코드 포맷팅` |
| `perf` | 성능 개선 | `perf(query): N+1 쿼리 최적화` |

### 커밋 메시지 예시

✅ **올바른 커밋 메시지**:
```
feat(auth): JWT 기반 로그인 API 구현

- AccessToken, RefreshToken 발급
- HttpOnly Cookie로 AccessToken 자동 설정
- 토큰 블랙리스트 기반 로그아웃
```

```
fix(schedule): 월별 제한 검증 로직 버그 수정

- 동시 신청 건수 계산 오류 수정
- 삭제된 일정은 카운트에서 제외하도록 변경
```

```
refactor(auth): AuthService 메서드 분리

- validateCredentials() 메서드 추가
- generateTokens() 메서드 추가
- 가독성 및 테스트 용이성 향상
```

❌ **잘못된 커밋 메시지**:
```
Update  # 너무 애매함
fix bug  # 어떤 버그인지 불명확
로그인 기능 추가  # 영어 사용 권장
feat: login api add  # type(scope) 형식 미준수
```

### 브랜치 전략

**브랜치 네이밍**:
- `main`: 배포 가능한 안정 버전
- `develop`: 개발 통합 브랜치
- `feature/{기능명}`: 기능 개발
- `fix/{버그명}`: 버그 수정
- `refactor/{리팩토링명}`: 리팩토링

**예시**:
```
feature/auth-login
feature/schedule-apply
fix/schedule-validation
refactor/auth-service
```

---

## 💻 사용 예시

### 예시 1: Service 클래스

```java
package com.better.CommuteMate.auth.application;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.better.CommuteMate.auth.controller.dto.LoginRequest;
import com.better.CommuteMate.auth.controller.dto.LoginResponse;
import com.better.CommuteMate.auth.controller.dto.RegisterRequest;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
import com.better.CommuteMate.global.security.jwt.JwtTokenProvider;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 새로운 사용자를 생성하고 저장합니다.
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 사용자 엔티티
     * @throws EmailAlreadyRegisteredException 이미 가입된 이메일인 경우
     */
    @Transactional
    public User register(RegisterRequest request) {
        validateEmailNotDuplicate(request.getEmail());

        User user = User.builder()
            .email(request.getEmail())
            .password(encodePassword(request.getPassword()))
            .name(request.getName())
            .roleCode(request.getRoleCode())
            .build();

        return userRepository.save(user);
    }

    /**
     * 로그인 처리 후 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보
     * @return AccessToken과 RefreshToken
     * @throws InvalidCredentialsException 이메일 또는 비밀번호가 틀린 경우
     */
    public LoginResponse login(LoginRequest request) {
        User user = findByEmail(request.getEmail());
        validatePassword(request.getPassword(), user.getPassword());

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken);
    }

    private void validateEmailNotDuplicate(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException(
                "이메일 또는 비밀번호가 올바르지 않습니다."
            ));
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException(
                "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }
    }
}
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [에러 처리 규약](./error-handling.md) - 예외 클래스 네이밍
- **필수**: [API 설계 규약](./api-conventions.md) - DTO 네이밍 규칙
- **참고**: [아키텍처 개요](../architecture/overview.md) - 패키지 구조

### 상위/하위 문서
- ⬆️ **상위**: [개발 규약 홈](./README.md)
- ➡️ **관련**:
  - [에러 처리 규약](./error-handling.md)
  - [API 설계 규약](./api-conventions.md)

### 코드 예시
- **Service**: `auth/application/AuthService.java`
- **Controller**: `auth/controller/AuthController.java`
- **Entity**: `domain/user/entity/User.java`
- **DTO**: `auth/controller/dto/RegisterRequest.java`
