# User 스키마

## 📑 목차
- [개요](#-개요)
- [테이블 구조](#-테이블-구조)
- [필드 설명](#-필드-설명)
- [관계](#-관계)
- [CodeType 연동](#-codetype-연동)
- [사용 예시](#-사용-예시)
- [주의사항](#-주의사항)
- [관련 문서](#-관련-문서)

---

## 📖 개요

User 테이블은 CommuteMate 시스템의 모든 사용자 정보를 관리하는 핵심 테이블입니다.

### 주요 특징
- **역할 기반 접근 제어**: CodeType.RL (Role) 코드로 사용자 역할 관리
- **JWT 인증**: refreshToken 필드로 토큰 기반 인증 지원
- **조직 소속**: `organizationId` 컬럼만 보유 (엔티티 연관관계 없음)
- **감사 추적**: 생성/수정 시간 및 작업자 추적

### 엔티티 위치
```
domain/user/entity/User.java
domain/user/repository/UserRepository.java
```

---

## 🗂️ 테이블 구조

### user 테이블

```sql
CREATE TABLE `user` (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    organization_id INT NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role_code CHAR(4) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by INT,
    updated_at DATETIME,
    updated_by INT,
    refresh_token VARCHAR(512),

    INDEX uq_user_email (email),
    FOREIGN KEY (organization_id) REFERENCES organization(organization_id)
);
```

### 인덱스
| 인덱스 명 | 컬럼 | 타입 | 목적 |
|----------|------|------|------|
| PRIMARY | user_id | Unique | 기본 키 |
| uq_user_email | email | Unique | 이메일 중복 방지 및 빠른 조회 |

---

## 📋 필드 설명

### 식별자
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| **user_id** | INT | PK, AUTO_INCREMENT, NOT NULL | 사용자 고유 식별자 |

### 기본 정보
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| **organization_id** | INT | NOT NULL | 소속 조직 ID (역정규화: 단순 컬럼) |
| **email** | VARCHAR(100) | NOT NULL, UNIQUE | 로그인 이메일 (중복 불가) |
| **password** | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 (BCrypt) |
| **name** | VARCHAR(50) | NOT NULL | 사용자 이름 |
| **role_code** | CHAR(4) | NOT NULL | 역할 코드 (RL01: 학생, RL02: 관리자) |

### 인증 정보
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| **refresh_token** | VARCHAR(512) | NULL | JWT 리프레시 토큰 |

### 감사 정보
| 필드 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| **created_at** | DATETIME | NOT NULL | 생성 일시 (자동 설정) |
| **created_by** | INT | NULL | 생성자 user_id |
| **updated_at** | DATETIME | NULL | 최종 수정 일시 (자동 갱신) |
| **updated_by** | INT | NULL | 최종 수정자 user_id |

---

## 🔗 관계

    ### ERD 다이어그램
    ```
    User (1) ──< (N) Faq (writer)
    User (1) ──< (N) Faq (lastEditor)
    ```

### 관계 상세

#### 1. Faq (1:N) - 작성자/수정자
```java
// Faq 엔티티에서
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "writer_id", nullable = false)
private User writer;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "last_editor_id", nullable = false)
private User lastEditor;
```
- **관계**: 한 명의 사용자는 여러 FAQ를 작성/수정할 수 있음
- **참조 필드**: `Faq.writer`, `Faq.lastEditor`

    #### 2. Organization 관계 (역정규화)

**설계 결정**: User 엔티티에서 Organization을 `organizationId (Long)` 컬럼으로만 저장

**이유**:
- **JPA 관계 설정 없음**: `@ManyToOne` 어노테이션을 사용하지 않음
- **N+1 쿼리 문제 방지**: Lazy Loading으로 인한 성능 저하 방지
- **조직 정보 조회 빈도가 낮음**: 대부분의 API에서 조직 정보가 불필요
- **성능 최적화**: 의도적인 역정규화 설계로 쿼리 최적화

**영향**:
- **조직 정보 조회 시 별도 쿼리 필요**: organizationId로 Organization을 수동 조회
- **참조 무결성**: 조직 삭제 시 애플리케이션 레벨에서 제약 조건 관리
- **유연한 쿼리**: 필요한 경우에만 조직 정보를 가져올 수 있음

**코드**:
```java
@Column(name = "organization_id", nullable = false)
private Long organizationId;  // 단순 컬럼, JPA 관계 없음
```

**코드 위치**: `src/main/java/com/better/CommuteMate/domain/user/entity/User.java:24-25`

**사용 예시**:
```java
// User에서 organizationId로 Organization 조회
User user = userRepository.findById(userId).orElseThrow();
Long orgId = user.getOrganizationId();

// 별도 쿼리로 Organization 조회
Organization org = organizationRepository.findById(orgId).orElseThrow();
```

---

## 🔢 CodeType 연동

### roleCode (RL: Role)
User의 역할을 정의하는 코드입니다.

| CodeType | Full Code | Code Name | Code Value | 설명 |
|----------|-----------|-----------|------------|------|
| RL01 | RL01 | STUDENT | 학생 | 일반 학생 사용자 (근무 신청/조회) |
| RL02 | RL02 | ADMIN | 관리자 | 시스템 관리자 (모든 권한) |

### 사용 방법
```java
// Entity에서
@Enumerated(EnumType.STRING)
@Column(name = "role_code", columnDefinition = "CHAR(4)", nullable = false)
private CodeType roleCode;

// 설정
user.setRoleCode(CodeType.RL01);  // 학생 역할

// 조회
if (user.getRoleCode() == CodeType.RL02) {
    // 관리자 권한 처리
}
```

### 권한 체계
| 역할 | 근무 일정 | 출퇴근 | FAQ 관리 | 사용자 관리 | 시스템 설정 |
|------|----------|--------|----------|-------------|-------------|
| **RL01 (학생)** | 본인 조회/신청 | 본인 출퇴근 | 조회만 | ❌ | ❌ |
| **RL02 (관리자)** | 전체 조회/관리 | 전체 조회 | 생성/수정/삭제 | ✅ | ✅ |

---

## 💻 사용 예시

### 1. 사용자 생성 (회원가입)
```java
@Service
public class AuthService {
    public User register(RegisterRequest request) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 생성
        User user = User.builder()
            .organizationId(request.getOrganizationId())
            .email(request.getEmail())
            .password(encodedPassword)
            .name(request.getName())
            .roleCode(request.getRoleCode())  // CodeType.RL01 or RL02
            .build();

        return userRepository.save(user);
    }
}
```

### 2. 이메일로 사용자 조회
```java
@Service
public class AuthService {
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.of(
                GlobalErrorCode.USER_NOT_FOUND,
                UserNotFoundResponseDetail.of(email)
            ));
    }
}
```

### 3. 리프레시 토큰 저장
```java
@Service
public class AuthService {
    public void saveRefreshToken(User user, String refreshToken) {
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
    }
}
```

### 4. 역할별 사용자 조회
```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByRoleCode(CodeType roleCode);

    // 관리자만 조회
    default List<User> findAllAdmins() {
        return findByRoleCode(CodeType.RL02);
    }
}
```

### 5. 조직별 사용자 조회
```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findByOrganizationId(Integer organizationId);
}
```

---

## ⚠️ 주의사항

### 1. 비밀번호 암호화
```java
// ❌ 잘못된 방법 - 평문 저장
user.setPassword("mypassword123");

// ✅ 올바른 방법 - BCrypt 암호화
String encoded = passwordEncoder.encode("mypassword123");
user.setPassword(encoded);
```

**중요**: 비밀번호는 반드시 BCrypt 등의 해시 알고리즘으로 암호화 후 저장해야 합니다.

### 2. 이메일 중복 체크
```java
// 회원가입 전 중복 확인 필수
if (userRepository.existsByEmail(email)) {
    throw new EmailAlreadyRegisteredException(...);
}
```

### 3. 리프레시 토큰 관리
- **저장**: 로그인 성공 시 refreshToken 필드에 저장
- **삭제**: 로그아웃 시 refreshToken을 NULL로 설정
- **갱신**: 토큰 만료 시 새 토큰 발급 후 업데이트

```java
// 로그아웃 처리
user.setRefreshToken(null);
userRepository.save(user);
```

### 4. 감사 정보 자동 설정
```java
// @PrePersist, @PreUpdate로 자동 설정되므로 수동 설정 불필요
// created_at, updated_at은 자동으로 관리됨
```

### 5. 역할 변경 시 권한 확인
```java
// 관리자만 다른 사용자의 역할을 변경할 수 있음
if (currentUser.getRoleCode() != CodeType.RL02) {
    throw new ForbiddenException("권한이 없습니다.");
}

targetUser.setRoleCode(newRoleCode);
userRepository.save(targetUser);
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [CodeType 시스템](./code-system.md) - roleCode 상세 설명
- **필수**: [인증 API](../api/auth.md) - 회원가입/로그인 API

### 상위/하위 문서
- ⬆️ **상위**: [데이터베이스 스키마 홈](./README.md)
- ➡️ **관련**:
  - [WorkSchedule 스키마](./schedule.md)
  - [WorkAttendance 스키마](./attendance.md)
  - [FAQ 스키마](./faq.md)

### 관련 API
- [인증 API](../api/auth.md): 회원가입, 로그인, 로그아웃

### 전체 ERD
https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757
