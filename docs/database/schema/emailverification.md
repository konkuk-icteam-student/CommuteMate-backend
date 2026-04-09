# EmailVerificationCode 엔티티

## 📋 목차
- [개요](#-개요)
- [테이블 구조](#-테이블-구조)
- [인증 프로세스](#-인증-프로세스)
- [제약사항](#-제약사항)
- [자동 정리](#-자동-정리)
- [API 연계](#-api-연계)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

회원가입 및 이메일 인증을 위한 **인증번호 관리 엔티티**입니다.

**주요 용도**:
- 이메일 인증번호 발송 및 저장
- 인증번호 검증 및 유효성 확인
- 만료된 인증 코드 자동 정리

---

## 🗄️ 테이블 구조

### email_verification_code 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 인증 코드 ID |
| code | VARCHAR(6) | NOT NULL | 6자리 인증번호 |
| email | VARCHAR(255) | NOT NULL | 인증 대상 이메일 |
| expires_at | DATETIME | NOT NULL | 만료 일시 (발급 후 5분) |
| created_at | DATETIME | NOT NULL | 생성 일시 |
| verified | BOOLEAN | NOT NULL, DEFAULT FALSE | 인증 완료 여부 |
| attempt_count | INT | NOT NULL, DEFAULT 0 | 인증 시도 횟수 |

**인덱스**:
- PRIMARY KEY: `id`
- INDEX: `email, verified` (복합 인덱스 - 미인증 코드 조회 최적화)
- INDEX: `expires_at` (만료 코드 정리 최적화)

**Java 코드**:
```java
@Entity
@Table(name = "email_verification_code")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 6, nullable = false)
    private String code;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
}
```

---

## 🔐 인증 프로세스

### 1. 인증번호 발송

**흐름**:
```
1. 사용자가 이메일 입력
   ↓
2. 6자리 랜덤 숫자 생성 (예: "123456")
   ↓
3. EmailService를 통해 이메일 발송
   ↓
4. DB에 저장 (만료 시간: 현재 + 5분)
   ↓
5. 발송 성공 응답
```

**코드 예시**:
```java
String code = String.format("%06d", new Random().nextInt(1000000));

EmailVerificationCode verificationCode = EmailVerificationCode.builder()
    .code(code)
    .email(email)
    .expiresAt(LocalDateTime.now().plusMinutes(5))
    .createdAt(LocalDateTime.now())
    .verified(false)
    .attemptCount(0)
    .build();

repository.save(verificationCode);
emailService.sendVerificationCode(email, code);
```

### 2. 인증번호 검증

**흐름**:
```
1. 사용자가 인증번호 입력
   ↓
2. 이메일 + 코드로 DB 조회
   ↓
3. 검증 단계:
   - 코드 존재 확인
   - 만료 시간 확인 (expires_at > now)
   - 시도 횟수 확인 (attempt_count < 5)
   - 인증 완료 여부 확인 (verified = false)
   ↓
4. 검증 성공 시:
   - verified = true로 업데이트
   - 회원가입 진행 허용
   ↓
5. 검증 실패 시:
   - attempt_count 증가
   - 적절한 에러 응답
```

**코드 예시**:
```java
EmailVerificationCode code = repository
    .findByEmailAndCodeAndVerifiedFalse(email, inputCode)
    .orElseThrow(() -> new InvalidVerificationCodeException());

// 만료 확인
if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new ExpiredVerificationCodeException();
}

// 시도 횟수 확인
if (code.getAttemptCount() >= 5) {
    throw new MaxVerificationAttemptsExceededException();
}

// 검증 성공
code.setVerified(true);
repository.save(code);
```

### 3. 회원가입 시 인증 확인

**흐름**:
```java
// 회원가입 전 이메일 인증 여부 확인
Optional<EmailVerificationCode> verified = repository
    .findByEmailAndVerifiedTrue(email);

if (verified.isEmpty()) {
    throw new EmailNotVerifiedException();
}

// 회원가입 진행
User user = User.builder()
    .email(email)
    .password(encodedPassword)
    .name(name)
    .build();
```

---

## ⚠️ 제약사항

### 1. 인증번호 유효 시간
- **시간**: 5분
- **계산**: `expiresAt = createdAt + 5분`
- **검증**: 인증 시도 시마다 만료 여부 확인

### 2. 최대 시도 횟수
- **횟수**: 5회
- **초과 시**: `MAX_VERIFICATION_ATTEMPTS_EXCEEDED` 에러
- **해결**: 새로운 인증번호 재요청 필요

### 3. 코드 형식
- **길이**: 6자리
- **형식**: 숫자만 (예: "123456")
- **생성**: `String.format("%06d", random.nextInt(1000000))`

### 4. 중복 발송 제한
- **제한**: 1분 이내 동일 이메일로 재발송 불가 (선택적 구현)
- **목적**: 스팸 방지 및 서버 부하 감소

### 5. 일회성 사용
- **verified = true**: 한 번 인증되면 재사용 불가
- **회원가입 완료 후**: 해당 인증 코드는 무효화

---

## 🧹 자동 정리

### EmailVerificationCleanupService

**역할**: 만료된 인증 코드를 자동으로 삭제

**실행 주기**: 매일 자정 (00:00)

**동작**:
```java
@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
public void cleanupExpiredCodes() {
    LocalDateTime now = LocalDateTime.now();
    repository.deleteByExpiresAtBefore(now);
    log.info("만료된 인증 코드 정리 완료");
}
```

**삭제 대상**:
- `expires_at < 현재 시간`인 모든 레코드
- 인증 완료 여부와 무관하게 삭제

**성능 고려**:
- 인덱스 활용: `expires_at` 컬럼 인덱스
- Batch Delete: 대량 삭제 시 배치 처리

---

## 🔗 API 연계

### 관련 엔드포인트

#### 1. POST /api/auth/send-verification-code
- **기능**: 인증번호 발송
- **Request**: `{ "email": "user@example.com" }`
- **Process**:
  1. 이메일 중복 확인 (User 테이블)
  2. 인증 코드 생성 및 저장
  3. 이메일 발송
- **Response**: `200 OK` (발송 성공)

#### 2. POST /api/auth/verify-code
- **기능**: 인증번호 검증
- **Request**: `{ "email": "user@example.com", "code": "123456" }`
- **Process**:
  1. 인증 코드 조회
  2. 만료/시도 횟수 검증
  3. `verified = true` 업데이트
- **Response**: `200 OK` (인증 성공)

#### 3. POST /api/auth/register
- **기능**: 회원가입
- **전제 조건**: 이메일 인증 완료 (`verified = true`)
- **Process**:
  1. 인증 여부 확인
  2. 회원가입 진행
  3. User 레코드 생성

### 관련 서비스

#### EmailService
- **위치**: `auth/application/EmailService.java`
- **역할**: 이메일 발송 (SMTP)
- **템플릿**: HTML 기반 이메일 템플릿

#### EmailVerificationCleanupService
- **위치**: `auth/application/EmailVerificationCleanupService.java`
- **역할**: 만료 코드 자동 정리
- **스케줄**: `@Scheduled(cron = "0 0 0 * * *")`

#### AuthService
- **위치**: `auth/application/AuthService.java`
- **역할**: 인증 로직 조율
- **기능**: 발송, 검증, 회원가입 통합

---

## 💡 사용 예시

### 1. 인증번호 생성 및 저장

```java
// 랜덤 6자리 숫자 생성
String code = String.format("%06d", new Random().nextInt(1000000));

// DB 저장
EmailVerificationCode verificationCode = EmailVerificationCode.builder()
    .code(code)
    .email("user@example.com")
    .expiresAt(LocalDateTime.now().plusMinutes(5))
    .createdAt(LocalDateTime.now())
    .verified(false)
    .attemptCount(0)
    .build();

repository.save(verificationCode);
```

### 2. 인증번호 검증

```java
// 이메일 + 코드로 조회
Optional<EmailVerificationCode> codeOpt = repository
    .findByEmailAndCodeAndVerifiedFalse(email, inputCode);

if (codeOpt.isEmpty()) {
    throw new InvalidVerificationCodeException("인증번호가 일치하지 않습니다.");
}

EmailVerificationCode code = codeOpt.get();

// 만료 확인
if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
    throw new ExpiredVerificationCodeException("인증번호가 만료되었습니다.");
}

// 시도 횟수 확인
if (code.getAttemptCount() >= 5) {
    throw new MaxVerificationAttemptsExceededException("인증 시도 횟수를 초과했습니다.");
}

// 검증 성공 - verified 플래그 업데이트
code.setVerified(true);
repository.save(code);
```

### 3. 시도 횟수 증가 (검증 실패 시)

```java
// 검증 실패 시 시도 횟수 증가
code.setAttemptCount(code.getAttemptCount() + 1);
repository.save(code);

if (code.getAttemptCount() >= 5) {
    throw new MaxVerificationAttemptsExceededException();
}
```

### 4. 회원가입 시 인증 확인

```java
// 이메일 인증 여부 확인
Optional<EmailVerificationCode> verified = repository
    .findByEmailAndVerifiedTrue(email);

if (verified.isEmpty()) {
    throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다.");
}

// 인증 완료 - 회원가입 진행
User user = userRepository.save(newUser);
```

### 5. 만료된 코드 정리

```java
@Scheduled(cron = "0 0 0 * * *")
public void cleanupExpiredCodes() {
    LocalDateTime now = LocalDateTime.now();
    int deleted = repository.deleteByExpiresAtBefore(now);
    log.info("만료된 인증 코드 {}개 삭제 완료", deleted);
}
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [인증 API](../../api/auth.md) - 인증번호 발송/검증 엔드포인트
- **필수**: [User 스키마](./user.md) - 회원가입 및 사용자 관리
- **참고**: [에러 처리 규약](../../conventions/error-handling.md) - 인증 관련 에러 코드

### 상위/하위 문서
- ⬆️ **상위**: [데이터베이스 스키마 홈](../README.md)
- ➡️ **관련**: [인증 API](../../api/auth.md#11-send-verification-code)

### 코드 위치
- **Entity**: `src/main/java/com/better/CommuteMate/domain/emailverification/entity/EmailVerificationCode.java`
- **Repository**: `src/main/java/com/better/CommuteMate/domain/emailverification/repository/EmailVerificationCodeRepository.java`
- **Service**: `src/main/java/com/better/CommuteMate/auth/application/EmailService.java`
- **Cleanup**: `src/main/java/com/better/CommuteMate/auth/application/EmailVerificationCleanupService.java`

---

## 📝 참고사항

### 보안 고려사항
1. **코드 예측 불가성**: 6자리 랜덤 숫자로 충분한 보안 제공
2. **재발송 제한**: 1분 이내 재발송 방지로 스팸 차단
3. **시도 횟수 제한**: 5회 제한으로 무차별 대입 공격 방지
4. **일회성 사용**: `verified = true` 후 재사용 불가

### 성능 최적화
1. **인덱스 활용**: `email`, `verified`, `expires_at` 복합 인덱스
2. **Batch Delete**: 만료 코드 대량 삭제 시 배치 처리
3. **자동 정리**: 스케줄러로 DB 크기 관리

### 확장 가능성
- SMS 인증 추가 (별도 테이블 또는 type 컬럼)
- 인증 방법 다양화 (이메일, SMS, OTP 등)
- 인증 코드 길이 설정 가능
- 재발송 쿨다운 타임 설정

---

**마지막 업데이트**: 2026-01-23
