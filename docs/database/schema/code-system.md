# 코드 시스템 (CodeType)

## 📑 목차
- [개요](#-개요)
- [CodeType Enum 구조](#-codetype-enum-구조)
- [코드 분류](#-코드-분류)
- [JPA 매핑 방식](#-jpa-매핑-방식)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate는 `CodeType` Enum을 통해 상태/역할/업무 타입 등의 코드 값을 타입 안전하게 관리합니다.

**파일 위치**: `src/main/java/com/better/CommuteMate/global/code/CodeType.java`

---

## 🔢 CodeType Enum 구조

```java
public enum CodeType {
    WS01("WS", "01", "REQUESTED", "신청"),
    WS02("WS", "02", "APPROVED", "승인"),
    WS03("WS", "03", "REJECTED", "반려"),
    WS04("WS", "04", "CANCELLED", "취소"),
    // ...

    private final String majorCode;
    private final String subCode;
    private final String codeName;
    private final String codeValue;

    public String getFullCode() { return majorCode + subCode; }
}
```

---

## 🗄️ Code 엔티티 구조

### 테이블 구조
Code 엔티티는 DB의 `code` 테이블과 매핑되며, CodeType Enum 값을 저장합니다.

```sql
CREATE TABLE code (
    full_code CHAR(4) NOT NULL,        -- CodeType (예: WS01)
    major_code CHAR(2) NOT NULL,       -- 대분류 (예: WS)
    sub_code CHAR(2) NOT NULL,         -- 소분류 (예: 01)
    code_name VARCHAR(100) NOT NULL,   -- 코드명 (예: REQUESTED)
    code_value VARCHAR(100) NOT NULL,  -- 코드값 (예: 신청)

    PRIMARY KEY (full_code),
    UNIQUE KEY uq_code_major_sub (major_code, sub_code),
    FOREIGN KEY (major_code, sub_code) REFERENCES code_sub(major_code, sub_code)
);
```

### 복합 외래 키 구조
Code 엔티티는 CodeSub와의 관계를 위해 **복합 외래 키**를 사용합니다.

#### 구현 코드
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumns({
    @JoinColumn(name = "major_code", referencedColumnName = "major_code",
                insertable = false, updatable = false),
    @JoinColumn(name = "sub_code", referencedColumnName = "sub_code",
                insertable = false, updatable = false)
})
private CodeSub codeSub;
```

**코드 위치**: `src/main/java/com/better/CommuteMate/domain/code/entity/Code.java:35-40`

#### 설명
- **major_code + sub_code**: 두 컬럼을 조합하여 CodeSub 참조
- **insertable = false, updatable = false**: 읽기 전용 관계
  - major_code와 sub_code는 이미 Code 테이블의 필드이므로 중복 저장 방지
  - JPA가 이 필드들을 통해 CodeSub를 참조할 수 있지만, INSERT/UPDATE 시에는 사용하지 않음
- **Lazy Loading**: 필요한 경우에만 CodeSub 정보 조회

#### 데이터 예시
```
Code 테이블:
full_code | major_code | sub_code | code_name  | code_value
----------|------------|----------|------------|------------
WS01      | WS         | 01       | REQUESTED  | 신청
WS02      | WS         | 02       | APPROVED   | 승인
TT01      | TT         | 01       | REGULAR    | 정기 업무

CodeSub 테이블:
major_code | sub_code | code_sub_name
-----------|----------|---------------
WS         | 01       | 근무신청-신청됨
WS         | 02       | 근무신청-승인됨
TT         | 01       | 업무-정기

→ Code.codeSub은 (major_code, sub_code) 조합으로 CodeSub를 참조
  예: Code(WS01)의 codeSub = CodeSub(WS, 01)
```

#### 사용 예시
```java
// Code 조회
Code code = codeRepository.findById(CodeType.WS01).orElseThrow();

// CodeSub 정보 접근 (Lazy Loading)
CodeSub codeSub = code.getCodeSub();  // DB 쿼리 발생
String subName = codeSub.getCodeSubName();  // "근무신청-신청됨"
```

---

## 📚 코드 분류

### WS - 근무 상태
| 코드 | 의미 |
|------|------|
| WS01 | 신청 |
| WS02 | 승인 |
| WS03 | 반려 |
| WS04 | 취소 |

### CR - 변경 요청 타입
| 코드 | 의미 |
|------|------|
| CR01 | 수정 요청 |
| CR02 | 삭제 요청 |

### CS - 변경 요청 상태
| 코드 | 의미 |
|------|------|
| CS01 | 대기 |
| CS02 | 승인 |
| CS03 | 거절 |

### CT - 출퇴근 체크 타입
| 코드 | 의미 |
|------|------|
| CT01 | 출근 체크 |
| CT02 | 퇴근 체크 |

### TT - 업무 타입
| 코드 | 의미 |
|------|------|
| TT01 | 정기 업무 |
| TT02 | 비정기 업무 |

### RL - 역할
| 코드 | 의미 |
|------|------|
| RL01 | 학생 |
| RL02 | 관리자 |

---

## 🧩 JPA 매핑 방식

코드 값은 `@Enumerated(EnumType.STRING)`으로 저장됩니다. 별도의 컨버터 클래스 없이 JPA의 기본 Enumerated 어노테이션을 활용합니다.

```java
@Enumerated(EnumType.STRING)
@Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
private CodeType statusCode;
```

### 매핑 원리
- **EnumType.STRING**: Enum의 `name()` 대신 전체 코드값(예: "WS01")을 저장
- **columnDefinition**: 데이터베이스에 CHAR(4)로 정의
- **장점**: 타입 안전성, 데이터베이스 독립성, 코드 가독성 향상

### 실제 사용 예시
```java
// 엔티티 정의
@Entity
public class WorkSchedule {
    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;
}

// 데이터 저장 (자동 변환: CodeType.WS01 → "WS01")
schedule.setStatusCode(CodeType.WS01);
scheduleRepository.save(schedule);  // DB에 "WS01" 저장

// 데이터 조회 (자동 변환: "WS01" → CodeType.WS01)
WorkSchedule loaded = scheduleRepository.findById(id);
CodeType code = loaded.getStatusCode();  // CodeType.WS01로 자동 변환됨
```

---

## 🔗 관련 문서

- [근무 일정 스키마](./schedule.md)
- [출퇴근 스키마](./attendance.md)
- [User 스키마](./user.md)
