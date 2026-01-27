# 설계 결정 기록 (Architecture Decision Records)

## 📑 목차
- [개요](#-개요)
- [ADR-001: 계층형 아키텍처 채택](#adr-001-계층형-아키텍처-채택)
- [ADR-002: JWT 기반 인증 방식](#adr-002-jwt-기반-인증-방식)
- [ADR-003: CodeType Enum 시스템](#adr-003-codetype-enum-시스템)
- [ADR-004: Response<T> 래퍼 패턴](#adr-004-responset-래퍼-패턴)
- [ADR-005: BasicException 예외 계층](#adr-005-basicexception-예외-계층)
- [ADR-006: ISO-8601 날짜/시간 표준](#adr-006-iso-8601-날짜시간-표준)
- [ADR-007: 모듈별 패키지 분리](#adr-007-모듈별-패키지-분리)
- [ADR-008: 부분 성공 응답 처리](#adr-008-부분-성공-응답-처리)
- [관련 문서](#-관련-문서)

---

## 📖 개요

이 문서는 CommuteMate 프로젝트의 주요 아키텍처 결정 사항과 그 근거를 기록합니다.
각 결정은 ADR (Architecture Decision Record) 형식으로 작성되어 있습니다.

**ADR 형식**:
- **제목**: 결정 내용 요약
- **상태**: Accepted, Deprecated, Superseded
- **컨텍스트**: 결정 당시의 상황 및 문제
- **결정**: 채택한 해결 방법
- **근거**: 선택 이유 및 고려 사항
- **대안**: 고려했던 다른 방법
- **결과**: 결정으로 인한 영향

---

## ADR-001: 계층형 아키텍처 채택

**상태**: ✅ Accepted
**날짜**: 2024-10-01
**결정자**: 개발팀

### 컨텍스트

Spring Boot 애플리케이션의 전체 구조를 어떻게 조직할 것인가에 대한 결정이 필요했습니다.
코드의 유지보수성, 확장성, 테스트 용이성을 확보하는 것이 주요 목표였습니다.

### 결정

**계층형 아키텍처(Layered Architecture)**를 채택하기로 결정했습니다.

**계층 구조**:
```
Controller (Presentation)
    ↓
Service (Application)
    ↓
Repository (Domain)
    ↓
Database (Infrastructure)
```

### 근거

1. **명확한 책임 분리**
   - 각 계층은 명확한 책임을 가짐
   - 코드의 목적을 쉽게 이해할 수 있음

2. **단방향 의존성**
   - 상위 계층만 하위 계층에 의존
   - 순환 참조 방지

3. **테스트 용이성**
   - 각 계층을 독립적으로 테스트 가능
   - Mock 객체를 사용한 단위 테스트 용이

4. **팀 협업 효율성**
   - 계층별로 작업 분담 가능
   - 병렬 개발 가능

5. **Spring Boot 모범 사례**
   - Spring 생태계에서 널리 사용되는 패턴
   - 풍부한 레퍼런스 및 커뮤니티 지원

### 대안

**1. 헥사고날 아키텍처 (Hexagonal Architecture)**
- 장점: 외부 시스템과의 결합도 최소화
- 단점: 복잡도 증가, 작은 프로젝트에는 과도함
- 기각 이유: CommuteMate 규모에 비해 복잡도가 높음

**2. 기능별 패키지 구조 (Feature-based)**
- 장점: 기능별로 코드가 응집
- 단점: 계층 간 관계가 불명확
- 기각 이유: 계층 간 의존성 관리가 어려움

### 결과

✅ **긍정적 영향**:
- 신규 개발자의 코드 이해도 향상
- 계층별 역할이 명확하여 코드 리뷰 용이
- 테스트 코드 작성이 체계적

⚠️ **부정적 영향**:
- 간단한 기능도 여러 파일에 분산
- 계층 간 DTO 변환 오버헤드

---

## ADR-002: JWT 기반 인증 방식

**상태**: ✅ Accepted
**날짜**: 2024-10-05
**결정자**: 보안팀

### 컨텍스트

사용자 인증 방식으로 세션 기반과 토큰 기반 중 선택이 필요했습니다.
RESTful API의 Stateless 특성과 확장성을 고려해야 했습니다.

### 결정

**JWT (JSON Web Token) 기반 인증**을 채택하기로 결정했습니다.

**구조**:
- **AccessToken**: 1시간 유효, Authorization 헤더로 전달 (Bearer {token})
- **RefreshToken**: 7일 유효, DB 저장 + 응답 본문으로 전달
- **토큰 블랙리스트**: 로그아웃 시 AccessToken 무효화

### 근거

1. **Stateless 특성**
   - 서버에 세션 저장 불필요 (RefreshToken만 DB에 저장)
   - 서버 확장 시 세션 동기화 문제 없음

2. **분산 시스템 적합**
   - 여러 서버 간 인증 상태 공유 용이
   - 마이크로서비스 아키텍처로 확장 가능

3. **모바일 앱 지원**
   - 모바일 앱에서도 동일한 토큰 사용 가능
   - Authorization 헤더 표준 사용으로 Cross-platform 호환성

4. **표준화**
   - RFC 7519 표준 준수
   - HTTP Authorization 헤더 표준 준수
   - 다양한 라이브러리 지원

5. **로그아웃 지원**
   - AccessToken 블랙리스트를 통한 안전한 로그아웃
   - RefreshToken은 DB에서만 관리하여 무효화 가능

### 대안

**1. 세션 기반 인증 (Session-based)**
- 장점: 서버에서 완전한 제어 가능
- 단점: 서버 확장 시 세션 동기화 필요
- 기각 이유: Stateless API 원칙에 위배

**2. OAuth 2.0**
- 장점: 산업 표준, 소셜 로그인 지원
- 단점: 복잡도 높음, 외부 의존성
- 기각 이유: 현재 요구사항에 비해 과도함

### 결과

✅ **긍정적 영향**:
- 서버 확장성 확보 (Stateless 아키텍처)
- RESTful API 원칙 준수
- 표준 HTTP 헤더 사용으로 도구 호환성 우수
- AccessToken 블랙리스트로 로그아웃 지원

⚠️ **부정적 영향**:
- RefreshToken DB 저장으로 추가 I/O 발생
- 토큰 탈취 가능성 (HTTPS 필수)
- 토큰 크기로 인한 오버헤드

---

## ADR-003: CodeType Enum 시스템

**상태**: ✅ Accepted
**날짜**: 2024-10-10
**결정자**: 개발팀

### 컨텍스트

시스템 전반에서 사용되는 코드 값(상태 코드, 역할 코드 등)을 어떻게 관리할 것인가에 대한 결정이 필요했습니다.
타입 안전성과 유지보수성을 확보하는 것이 목표였습니다.

### 결정

**CodeType Enum 시스템**을 도입하기로 결정했습니다.

**구조**:
```java
public enum CodeType {
    // 근무 상태 (WS)
    WS01("WS", "01", "REQUESTED", "신청"),
    WS02("WS", "02", "APPROVED", "승인"),
    WS03("WS", "03", "REJECTED", "반려"),
    WS04("WS", "04", "CANCELLED", "취소"),

    // 역할 (RL)
    RL01("RL", "01", "STUDENT", "학생"),
    RL02("RL", "02", "ADMIN", "관리자");

    private final String majorCode;
    private final String subCode;
    private final String codeName;
    private final String codeValue;

    public String getFullCode() {
        return majorCode + subCode;  // "WS01"
    }
}
```

### 근거

1. **타입 안전성**
   - 컴파일 타임에 코드 값 검증
   - 잘못된 코드 값 사용 방지

2. **IDE 지원**
   - 자동완성 기능 활용
   - 리팩토링 시 안전한 변경

3. **중앙 관리**
   - 모든 코드 값을 한 곳에서 관리
   - 코드 추가/변경 시 영향 범위 파악 용이

4. **JPA 통합**
   - `@Enumerated(EnumType.STRING)`로 Enum을 문자열로 저장
   - DB에는 문자열, Java에서는 Enum 사용

### 대안

**1. 문자열 상수 (String Constants)**
```java
public class StatusCode {
    public static final String REQUESTED = "WS01";
    public static final String APPROVED = "WS02";
}
```
- 장점: 단순함
- 단점: 타입 안전하지 않음, 오타 가능
- 기각 이유: 타입 안전성 부족

**2. 데이터베이스 참조 (Database Lookup)**
- 장점: 코드 변경 시 재배포 불필요
- 단점: DB 조회 오버헤드, 타입 안전하지 않음
- 기각 이유: 성능 및 타입 안전성 문제

### 결과

✅ **긍정적 영향**:
- 코드 값 오류 90% 감소
- 코드 리뷰 시 코드 값 검증 불필요
- IDE 자동완성으로 개발 속도 향상

⚠️ **부정적 영향**:
- 새로운 코드 추가 시 Enum 수정 필요
- DB 마이그레이션과 동기화 필요

---

## ADR-004: Response<T> 래퍼 패턴

**상태**: ✅ Accepted
**날짜**: 2024-10-12
**결정자**: API 설계팀

### 컨텍스트

API 응답 형식을 통일하여 클라이언트의 응답 처리를 단순화할 필요가 있었습니다.
성공/실패 여부를 명확히 전달하고, 일관된 에러 처리를 제공하는 것이 목표였습니다.

### 결정

**Response<T> 래퍼 패턴**을 모든 API 응답에 적용하기로 결정했습니다.

**구조**:
```java
public class Response<T> {
    private boolean isSuccess;
    private String message;
    private T details;
}
```

**사용 예시**:
```java
// 성공 응답
Response.success("로그인 성공", loginResponse);

// 실패 응답
Response.error("사용자를 찾을 수 없습니다.", errorDetail);
```

### 근거

1. **응답 형식 통일**
   - 모든 API가 동일한 구조로 응답
   - 클라이언트의 응답 파싱 로직 단순화

2. **성공/실패 명확화**
   - `isSuccess` 필드로 즉시 판단 가능
   - HTTP 상태 코드와 별도로 비즈니스 성공 여부 전달

3. **메시지 전달**
   - 사용자에게 표시할 메시지 제공
   - 다국어 지원 시 메시지만 변경 가능

4. **타입 안전성**
   - 제네릭을 통한 타입 안전한 응답
   - 컴파일 타임에 타입 검증

### 대안

**1. 직접 응답 (Direct Response)**
```java
@GetMapping("/users/{id}")
public UserDTO getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```
- 장점: 단순함, 오버헤드 없음
- 단점: 성공/실패 구분 어려움, 메시지 전달 불가
- 기각 이유: 일관된 에러 처리 불가

**2. Spring의 ResponseEntity**
```java
@GetMapping("/users/{id}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
}
```
- 장점: Spring 표준
- 단점: 비즈니스 성공/실패 구분 어려움
- 기각 이유: 도메인 메시지 전달 어려움

### 결과

✅ **긍정적 영향**:
- 클라이언트의 응답 처리 로직 단순화
- 에러 메시지를 사용자에게 직접 표시 가능
- API 문서 작성 용이

⚠️ **부정적 영향**:
- 응답 크기 증가 (약 20-30 bytes)
- 래퍼 제거가 필요한 경우 불편함

---

## ADR-005: BasicException 예외 계층

**상태**: ✅ Accepted
**날짜**: 2024-10-15
**결정자**: 개발팀

### 컨텍스트

애플리케이션 전반의 예외 처리를 체계화하고, 일관된 에러 응답을 제공할 필요가 있었습니다.
도메인별 예외를 구조화하여 관리하는 것이 목표였습니다.

### 결정

**BasicException을 베이스로 하는 예외 계층**을 구축하기로 결정했습니다.

**구조**:
```java
public abstract class BasicException extends RuntimeException {
    private final String userMessage;
    private final String logMessage;
    private final CustomErrorCode errorCode;
}

// 도메인별 예외
public class UserNotFoundException extends BasicException {
    public UserNotFoundException(Long userId, String message) {
        super(message, "User not found: " + userId, GlobalErrorCode.USER_NOT_FOUND);
    }
}
```

### 근거

1. **예외 구조화**
   - 모든 커스텀 예외가 BasicException 확장
   - 공통 속성 및 동작 상속

2. **메시지 분리**
   - `userMessage`: 사용자에게 표시할 메시지
   - `logMessage`: 로그에 기록할 상세 메시지
   - 민감 정보 노출 방지

3. **에러 코드 통합**
   - `CustomErrorCode` 인터페이스로 표준화
   - 도메인별 에러 코드 Enum 관리

4. **전역 예외 처리**
   - `GlobalExceptionHandler`에서 일관된 처리
   - HTTP 상태 코드 매핑

### 대안

**1. 표준 Java 예외 사용**
- 장점: 별도 구현 불필요
- 단점: 도메인 컨텍스트 표현 어려움
- 기각 이유: 비즈니스 의미 전달 불가

**2. 체크 예외 (Checked Exception)**
- 장점: 컴파일 타임에 예외 처리 강제
- 단점: 코드 복잡도 증가, 보일러플레이트 코드 증가
- 기각 이유: Spring 프레임워크 모범 사례와 불일치

### 결과

✅ **긍정적 영향**:
- 예외 처리 로직 일관성 확보
- 에러 응답 형식 통일
- 로그 분석 용이성 향상

⚠️ **부정적 영향**:
- 새로운 예외 타입 추가 시 클래스 생성 필요
- 예외 계층 구조 학습 곡선

---

## ADR-006: ISO-8601 날짜/시간 표준

**상태**: ✅ Accepted
**날짜**: 2024-10-18
**결정자**: API 설계팀

### 컨텍스트

API에서 날짜와 시간을 표현하는 형식을 통일할 필요가 있었습니다.
다양한 타임존을 지원하고, 국제 표준을 따르는 것이 목표였습니다.

### 결정

**ISO-8601 표준**을 모든 날짜/시간 표현에 사용하기로 결정했습니다.

**형식**:
- 날짜: `yyyy-MM-dd` (예: `2024-10-18`)
- 시간: `HH:mm:ss` (예: `14:30:00`)
- 날짜시간: `yyyy-MM-dd'T'HH:mm:ss` (예: `2024-10-18T14:30:00`)

### 근거

1. **국제 표준**
   - ISO 8601 국제 표준 준수
   - 전 세계적으로 인정받는 형식

2. **명확성**
   - 월/일 순서 혼동 방지
   - 타임존 명시 가능

3. **JSON 호환성**
   - JSON 표준에서 권장하는 형식
   - JavaScript Date 객체와 호환

4. **정렬 용이성**
   - 문자열 정렬로 시간순 정렬 가능
   - 데이터베이스 인덱싱 효율적

### 대안

**1. Unix Timestamp**
- 장점: 숫자로 표현, 크기 작음
- 단점: 가독성 떨어짐, 타임존 정보 없음
- 기각 이유: 사람이 읽기 어려움

**2. 로컬 형식 (yyyy/MM/dd)**
- 장점: 한국에서 친숙함
- 단점: 국제 표준 아님, 정렬 어려움
- 기각 이유: 국제화 대응 어려움

### 결과

✅ **긍정적 영향**:
- 날짜/시간 파싱 에러 감소
- 국제화 지원 용이
- API 문서 작성 명확

⚠️ **부정적 영향**:
- 한국 사용자에게 다소 생소함
- 프론트엔드에서 변환 로직 필요

---

## ADR-007: 모듈별 패키지 분리

**상태**: ✅ Accepted
**날짜**: 2024-10-20
**결정자**: 아키텍처팀

### 컨텍스트

프로젝트 규모가 커짐에 따라 코드 조직 방식을 재정립할 필요가 있었습니다.
도메인별로 독립적인 모듈을 구성하여 확장성을 확보하는 것이 목표였습니다.

### 결정

**도메인 중심의 모듈별 패키지 분리**를 채택하기로 결정했습니다.

**구조**:
```
src/main/java/com/better/CommuteMate/
├── auth/              # 인증 모듈
│   ├── controller/
│   └── application/
├── schedule/          # 근무 일정 모듈
│   ├── controller/
│   └── application/
└── domain/            # 도메인 엔티티
    ├── user/
    ├── schedule/
    └── ...
```

### 근거

1. **도메인 응집도**
   - 관련된 기능이 한 모듈에 모임
   - 코드 이해도 향상

2. **독립적 개발**
   - 모듈별로 독립적인 개발 가능
   - 팀별 작업 분담 용이

3. **확장성**
   - 새로운 모듈 추가 용이
   - 마이크로서비스로 전환 시 유리

4. **의존성 관리**
   - 모듈 간 의존성 명확화
   - 순환 참조 방지

### 대안

**1. 계층별 패키지 분리**
```
src/main/java/com/better/CommuteMate/
├── controller/
├── service/
├── repository/
└── entity/
```
- 장점: 계층이 명확함
- 단점: 도메인 코드가 분산됨
- 기각 이유: 도메인 응집도 낮음

**2. 단일 패키지**
- 장점: 단순함
- 단점: 프로젝트 규모 증가 시 관리 어려움
- 기각 이유: 확장성 부족

### 결과

✅ **긍정적 영향**:
- 도메인별 코드 이해도 향상
- 모듈 간 의존성 명확화
- 팀 협업 효율성 증가

⚠️ **부정적 영향**:
- 초기 구조 설정 복잡도 증가
- 공통 코드 관리 필요 (global 패키지)

---

## ADR-008: 부분 성공 응답 처리

**상태**: ✅ Accepted
**날짜**: 2024-10-25
**결정자**: API 설계팀

### 컨텍스트

근무 일정 일괄 신청과 같이 여러 항목을 동시에 처리할 때, 일부만 성공하는 경우를 어떻게 처리할 것인가에 대한 결정이 필요했습니다.

### 결정

**HTTP 207 Multi-Status**를 사용하여 부분 성공을 표현하기로 결정했습니다.

**응답 구조**:
```java
// 모든 성공: HTTP 200
{
  "isSuccess": true,
  "message": "모든 일정이 신청되었습니다.",
  "details": {
    "success": [...]
  }
}

// 일부 성공: HTTP 207
{
  "isSuccess": true,
  "message": "일부 일정이 신청되었습니다.",
  "details": {
    "success": [...],
    "failure": [...]
  }
}

// 모든 실패: HTTP 422
{
  "isSuccess": false,
  "message": "모든 일정 신청이 실패했습니다.",
  "details": {
    "failure": [...]
  }
}
```

### 근거

1. **상태 명확화**
   - HTTP 상태 코드로 결과 즉시 파악
   - 부분 성공을 표준 방식으로 전달

2. **클라이언트 대응**
   - 성공/실패 항목을 구분하여 처리 가능
   - 사용자에게 정확한 피드백 제공

3. **트랜잭션 정책**
   - 일부 실패 시에도 성공 항목 유지
   - 사용자 경험 향상

4. **HTTP 표준 준수**
   - WebDAV (RFC 4918)에서 정의된 표준
   - RESTful API 모범 사례

### 대안

**1. All-or-Nothing (전체 롤백)**
- 장점: 트랜잭션 일관성 유지
- 단점: 사용자 경험 저하, 재시도 부담
- 기각 이유: 사용자 편의성 부족

**2. 항상 HTTP 200 반환**
- 장점: 구현 단순
- 단점: 상태 파악 어려움
- 기각 이유: HTTP 상태 코드 의미 상실

### 결과

✅ **긍정적 영향**:
- 사용자 경험 개선 (일부 성공 항목 유지)
- 명확한 결과 전달
- 클라이언트의 에러 처리 용이

⚠️ **부정적 영향**:
- 클라이언트가 207 상태 코드 처리 필요
- 트랜잭션 관리 복잡도 증가

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [아키텍처 개요](./overview.md) - 설계 결정의 구현 방식
- **필수**: [코드베이스 구조](./codebase-structure.md) - 설계 결정의 물리적 구조
- **필수**: [개발 규약](../conventions/README.md) - 설계 결정에 따른 코딩 규칙
- **참고**: [API 문서](../api/README.md) - API 설계 결정의 구체적 예시

### 상위/하위 문서
- ⬆️ **상위**: [아키텍처 README](./README.md)
- ➡️ **관련**:
  - [아키텍처 개요](./overview.md)
  - [코드베이스 구조](./codebase-structure.md)

### 실무 적용
- **새로운 아키텍처 결정**: ADR 형식으로 문서화하고 이 파일에 추가
- **기존 결정 변경**: 상태를 Superseded로 변경하고 새로운 ADR 작성
- **의사 결정 시**: 이 문서의 기존 결정 사항 참고
